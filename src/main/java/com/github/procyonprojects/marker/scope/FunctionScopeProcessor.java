package com.github.procyonprojects.marker.scope;

import com.github.procyonprojects.marker.handler.FunctionInsertHandler;
import com.github.procyonprojects.marker.handler.TypeInsertHandler;
import com.goide.completion.GoCompletionUtil;
import com.goide.completion.GoLookupElementOptions;
import com.goide.completion.GoMLCompletionFeatures;
import com.goide.completion.GoParameterNameDecorator;
import com.goide.psi.*;
import com.goide.psi.impl.*;
import com.goide.psi.impl.expectedTypes.GoExpectedTypes;
import com.goide.refactor.GoNameSuggestionProvider;
import com.goide.stubs.index.GoAllPublicNamesIndex;
import com.goide.vendor.GoVendoringUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.intellij.util.ObjectUtils;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FunctionScopeProcessor  extends GoScopeProcessor {
    private static final BasicInsertHandler<LookupElementDecorator<LookupElement>> BASIC_INSERT_HANDLER = new BasicInsertHandler();
    @NotNull
    private final CompletionResultSet myResult;
    private final boolean myForTypes;
    private final boolean myVendoringEnabled;
    private final boolean mySuggestParameterName;
    private final GoExpectedTypes myExpectedTypes;
    private final Set<String> myProcessedNames;
    private final CompletionParameters myParameters;
    @NotNull
    private final Set<String> myUsedNames;
    private final boolean myIgnoreVisibilityRules;
    private boolean sideEffectImportHandled = false;

    public FunctionScopeProcessor(@NotNull CompletionResultSet result, @NotNull PsiFile originalFile, boolean forTypes, @NotNull GoExpectedTypes expectedTypes, @NotNull CompletionParameters parameters) {
        super();
        this.myProcessedNames = new HashSet();
        this.myResult = result;
        this.myForTypes = forTypes;
        this.myVendoringEnabled = GoVendoringUtil.isVendoringEnabled(originalFile);
        this.myExpectedTypes = expectedTypes;
        this.myParameters = parameters;
        PsiElement parent = this.myParameters.getPosition().getParent();
        this.mySuggestParameterName = GoParameterNameDecorator.shouldSuggestNamesInParameters(parent);
        this.myIgnoreVisibilityRules = originalFile instanceof GoCodeFragment && ((GoCodeFragment) originalFile).isIgnoreVisibilityRules();
        this.myUsedNames = (Set) (this.mySuggestParameterName ? GoNameSuggestionProvider.getNamesInContext(PsiTreeUtil.getParentOfType(parent, GoSignature.class)) : Collections.emptySet());
    }

    public boolean execute(@NotNull PsiElement o, @NotNull ResolveState state) {
        return this.execute(o, state, (String) null);
    }

    private boolean execute(@NotNull PsiElement o, @NotNull ResolveState state, @Nullable String qualifier) {
        if (o instanceof GoNamedElement && this.accept(o)) {
            this.addElement((GoNamedElement) o, state, qualifier);
        }

        if (!sideEffectImportHandled) {
            GoFile goFile = (GoFile) o.getContainingFile();
            List<GoImportSpec> sideEffectImports = new ArrayList<>(goFile.getImportMap().get("_"));
            for (GoImportSpec sideEffectImport : sideEffectImports) {
                this.addElement(sideEffectImport, state, sideEffectImport.getPath());
            }
            sideEffectImportHandled = true;
        }

        return true;
    }

    private void addElement(@NotNull GoNamedElement o, @NotNull ResolveState state, @Nullable String qualifier) {
        LookupElement lookup = this.createLookupElement(o, state, qualifier);
        if (lookup != null) {
            String lookupString = lookup.getLookupString();
            if (!this.myProcessedNames.contains(lookupString)) {
                this.myResult.addElement(lookup);
                this.myProcessedNames.add(lookupString);
                GoMLCompletionFeatures.addNamedElementKind(lookup, o);
            }
        }

    }

    @Nullable
    private static GoType getPointerType(@NotNull GoNamedElement element, @NotNull ResolveState state, boolean forTypes) {
        GoNamedElement originalElement = (GoNamedElement) CompletionUtil.getOriginalOrSelf(element);
        GoType type;
        if (originalElement instanceof GoTypeSpec) {
            type = GoElementFactory.createTypeCheap(element.getProject(), "*" + originalElement.getName(), originalElement);
            if (forTypes || ((GoTypeSpec) originalElement).getSpecType().getType() instanceof GoCompositeType) {
                return type;
            }
        } else {
            if (element instanceof GoSignatureOwner) {
                return null;
            }

            type = originalElement.getGoType(state);
            if (type != null) {
                Object context;
                String pointerTypeText;
                if (GoTypeUtil.isNamedType(type)) {
                    GoTypeSpec resolvedType = (GoTypeSpec) ObjectUtils.tryCast(type.resolve(state), GoTypeSpec.class);
                    pointerTypeText = resolvedType != null ? "*" + resolvedType.getName() : null;
                    context = resolvedType;
                } else {
                    pointerTypeText = "*" + type.getText();
                    context = originalElement;
                }

                return pointerTypeText != null ? GoElementFactory.createTypeCheap(element.getProject(), pointerTypeText, (PsiElement) context) : null;
            }
        }

        return null;
    }

    @Nullable
    private LookupElement createLookupElement(@NotNull GoNamedElement element, @NotNull ResolveState state, @Nullable String qualifier) {
        if (element.isBlank()) {
            return null;
        } else if (element instanceof GoImportSpec) {
            return this.createLookupElementForImportSpec(element, state);
        } else {
            String name = element.getName();
            if (StringUtil.isEmpty(name)) {
                return null;
            } else {
                LookupElement fastLookupElement = this.createLookupElementFast(element, state, qualifier, name);
                if (!this.myResult.getPrefixMatcher().prefixMatches(fastLookupElement)) {
                    return null;
                } else {
                    boolean smartCompletion = this.myParameters.getCompletionType() == CompletionType.SMART;
                    GoNamedElement original = (GoNamedElement) CompletionUtil.getOriginalOrSelf(element);
                    boolean isTypeCompatible = this.isTypeCompatible(original, state);
                    boolean isPointerTypeCompatible = false;
                    boolean isDerefTypeCompatible = false;
                    if (smartCompletion && !isTypeCompatible) {
                        GoType pointerType = getPointerType(element, state, this.myForTypes);
                        isPointerTypeCompatible = pointerType != null && this.myExpectedTypes.areCompatibleWith(pointerType, (GoTypeOwner) null, true, false, state);
                        if (!this.myForTypes && !isPointerTypeCompatible) {
                            GoType type = original.getGoType(state);
                            if (type instanceof GoPointerType) {
                                GoType derefType = ((GoPointerType) type).getType();
                                isDerefTypeCompatible = derefType != null && this.myExpectedTypes.areCompatibleWith(derefType, (GoTypeOwner) null, true, false, state);
                            }
                        }

                        if (!isPointerTypeCompatible && !isDerefTypeCompatible) {
                            return null;
                        }
                    }

                    return this.createLookupElement(element, state, qualifier, name, isTypeCompatible, isPointerTypeCompatible, isDerefTypeCompatible, false);
                }
            }
        }
    }

    @NotNull
    private LookupElement createLookupElementFast(@NotNull GoNamedElement element, @NotNull ResolveState state, @Nullable String qualifier, @NotNull String name) {
        return this.createLookupElement(element, state, qualifier, name, false, false, false, true);
    }

    @NotNull
    private LookupElement createLookupElement(@NotNull GoNamedElement element, @NotNull ResolveState state, @Nullable String qualifier, @NotNull String name, boolean isTypeCompatible, boolean isPointerTypeCompatible, boolean isDerefTypeCompatible, boolean fast) {
        int priorityDelta = this.getPriorityDelta(element, isTypeCompatible || isPointerTypeCompatible || isDerefTypeCompatible);
        LookupElement var10000;
        if (element instanceof GoTypeSpec) {
            GoLookupElementOptions options = (new GoLookupElementOptions()).setLookupString(prepend(name, qualifier)).setPriorityDelta((double) priorityDelta);
            if (this.myForTypes) {
                if (isPointerTypeCompatible) {
                    options.setLookupString("*" + options.getLookupString());
                }
            } else {
                options.setTakeAddress(isPointerTypeCompatible);
                options.setPriority((double) (35 + priorityDelta));
            }

            if (options.getInsertHandler() == null) {
                options.setInsertHandler(TypeInsertHandler.TYPE_INSERT_HANDLER);
            }

            LookupElement origin = GoCompletionUtil.createTypeLookupElement((GoTypeSpec) element, options);
            return origin;
        } else if (element instanceof GoLabelDefinition) {
            var10000 = GoCompletionUtil.createLabelLookupElement((GoLabelDefinition) element, name, priorityDelta);
            return var10000;
        } else if (element instanceof GoFieldDefinition) {
            var10000 = GoCompletionUtil.createFieldLookupElement((GoFieldDefinition) element, (new GoLookupElementOptions()).setTakeAddress(isPointerTypeCompatible).setDereference(isDerefTypeCompatible).setPriorityDelta((double) priorityDelta));
            return var10000;
        } else {
            GoType type = element.getGoType(state);
            if (type instanceof GoFunctionType) {
                return this.createLookupElementForElementOfFunctionType(state, element, name, isPointerTypeCompatible, priorityDelta, qualifier, fast);
            } else {
                GoLookupElementOptions options = (new GoLookupElementOptions()).setLookupString(prepend(name, qualifier)).setInsertHandler(GoCompletionUtil.Lazy.VARIABLE_OR_FUNCTION_INSERT_HANDLER).setResolveState(state).setTakeAddress(isPointerTypeCompatible).setDereference(isDerefTypeCompatible).setPriorityDelta((double) priorityDelta);
                var10000 = GoCompletionUtil.createVariableLikeLookupElement(element, options);
                return var10000;
            }
        }
    }

    @Nullable
    private LookupElement createLookupElementForImportSpec(@NotNull GoNamedElement element, @NotNull ResolveState state) {
        String name = (String) state.get(GoReferenceBase.ACTUAL_NAME);
        if (name == null) {
            name = ((GoImportSpec)element).getPath();
        }

        Collection<GoPackage> packages = ((GoImportSpec) element).resolve(state);
        String alias = ((GoImportSpec) element).getAlias();
        Iterator var7 = packages.iterator();

        while (var7.hasNext()) {
            GoPackage aPackage = (GoPackage) var7.next();
            String packageName = aPackage.getName();
            final String packageNameDot = packageName + ".";
            CommonProcessors.CollectProcessor<String> collectPackageNames = new CommonProcessors.CollectProcessor<String>() {
                protected boolean accept(String s) {
                    return s.startsWith(packageNameDot);
                }
            };
            Project project = element.getProject();
            StubIndex.getInstance().processAllKeys(GoAllPublicNamesIndex.ALL_PUBLIC_NAMES, project, collectPackageNames);
            GlobalSearchScope packageScope = aPackage.getScope(element.getContainingFile());
            String qualifierToUse = alias == null ? packageName : alias;

            if ("_".equals(qualifierToUse)) {
                qualifierToUse = ((GoImportSpec)element).getPath();
            }

            Iterator var15 = collectPackageNames.getResults().iterator();

            String finalQualifierToUse = qualifierToUse;
            while (var15.hasNext()) {
                String elementName = (String) var15.next();
                StubIndex.getInstance().processElements(GoAllPublicNamesIndex.ALL_PUBLIC_NAMES, elementName, project, packageScope, (IdFilter) null, GoNamedElement.class, (it) -> {
                    if (!(it instanceof GoMethodDeclaration) && GoPsiUtil.isTopLevelDeclaration(it)) {
                        this.execute(it, state, finalQualifierToUse);
                    }

                    return true;
                });
            }
        }

        return null;
    }

    @NotNull
    private LookupElement createLookupElementForElementOfFunctionType(@NotNull ResolveState state, @NotNull GoNamedElement o, @NotNull String name, boolean takeAddress, int priorityDelta, @Nullable String qualifier, boolean fast) {
        LookupElement lookup;
        GoLookupElementOptions options;
        if (o instanceof GoNamedSignatureOwner) {
            options = (new GoLookupElementOptions()).setLookupString(prepend(name, qualifier)).setPriorityDelta((double) priorityDelta);
            options.setInsertHandler(FunctionInsertHandler.FUNCTION_INSERT_HANDLER);
            lookup = GoCompletionUtil.createFunctionOrMethodLookupElement(o, options);
        } else {
            options = (new GoLookupElementOptions()).setLookupString(prepend(name, qualifier)).setInsertHandler(GoCompletionUtil.Lazy.VARIABLE_OR_FUNCTION_INSERT_HANDLER).setResolveState(state).setTakeAddress(takeAddress).setPriorityDelta((double) priorityDelta);
            lookup = GoCompletionUtil.createVariableLikeLookupElement(o, options);
        }

        if (fast) {
            return lookup;
        } else {
            LookupElementDecorator var10000;
            if (this.myExpectedTypes.containFunctionType(state) && !this.isFunctionResultTypeCompatible(o.getGoType(state), state)) {
                var10000 = LookupElementDecorator.withInsertHandler(lookup, BASIC_INSERT_HANDLER);
                return var10000;
            } else {
                if (this.myExpectedTypes.containInterfaceType(state) || this.myExpectedTypes.containDocOnlyAnyType(state)) {
                    GoType type = o.getGoType(state);
                    if (type instanceof GoFunctionType && ((GoFunctionType) type).getResultType() instanceof GoLightType.LightVoidType) {
                        var10000 = LookupElementDecorator.withInsertHandler(lookup, BASIC_INSERT_HANDLER);
                        return var10000;
                    }
                }
                return lookup;
            }
        }
    }

    @NotNull
    private static String prepend(@NotNull String name, @Nullable String qualifier) {
        String var10000 = (qualifier == null ? "" : qualifier + ".") + name;
        return var10000;
    }

    private int getPriorityDelta(@NotNull GoNamedElement o, boolean isTypeCompatible) {
        if (isTypeCompatible && o instanceof GoTypeSpec && !this.myForTypes) {
            GoType type = ((GoTypeSpec) o).getSpecType().getType();
            return type != null && GoPsiImplUtil.isValidLiteralType(type.getUnderlyingType(o)) ? 5 : 4;
        } else {
            GoFile file = ((GoNamedElement) CompletionUtil.getOriginalOrSelf(o)).getContainingFile();
            if (GoPsiImplUtil.isBuiltinFile(file) && "nil".equals(o.getName())) {
                return isTypeCompatible ? 4 : 0;
            } else {
                return isTypeCompatible ? 5 : 0;
            }
        }
    }

    private boolean isTypeCompatible(@NotNull GoNamedElement o, @NotNull ResolveState state) {
        if (o instanceof GoTypeSpec) {
            GoType type = ((GoTypeSpec) o).getSpecType();
            return this.myExpectedTypes.areCompatibleWith((GoType) CompletionUtil.getOriginalOrSelf(type), (GoTypeOwner) null, true, false, state);
        } else {
            return this.myExpectedTypes.areCompatibleWith((GoTypeOwner) CompletionUtil.getOriginalOrSelf(o), true, false, state) || this.isFunctionResultTypeCompatible(o.getGoType(state), state);
        }
    }

    private boolean isFunctionResultTypeCompatible(@Nullable GoType type, @NotNull ResolveState state) {
        GoType resultType = type instanceof GoFunctionType ? ((GoFunctionType) type).getResultType() : null;
        return resultType != null && this.myExpectedTypes.areCompatibleWith((GoType) CompletionUtil.getOriginalOrSelf(resultType), (GoTypeOwner) null, true, false, state);
    }

    protected boolean accept(@NotNull PsiElement e) {
        if (!(e instanceof GoFunctionDeclarationImpl) && !(e instanceof GoImportSpecImpl)) {
            return false;
        }
        return !GoPsiUtil.isUnexportedAndFromOtherPackage(e, this.myParameters.getOriginalFile()) || this.myIgnoreVisibilityRules && (GoPsiImplUtil.isFieldDefinition(e) || e instanceof GoVarOrConstDefinition);
    }

    public boolean isCompletion() {
        return true;
    }
}