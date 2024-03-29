package com.github.procyonprojects.marker.index;

import com.github.procyonprojects.marker.comment.ImportParser;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.github.procyonprojects.marker.metadata.v1.Marker;
import com.goide.GoFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MarkerImportIndex extends FileBasedIndexExtension<String, String> {

    public static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);
    public static final ID<String, String> KEY = ID.create("go.marker.imports");

    @Override
    public @NotNull ID<String, String> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, String, FileContent> getIndexer() {
        return new DataIndexer<String, String, FileContent>() {
            @Override
            public @NotNull Map<String, String> map(@NotNull FileContent inputData) {
                Optional<Marker> marker = METADATA_PROVIDER.getImportMarker();
                if (marker.isEmpty()) {
                    return Collections.emptyMap();
                }

                if (inputData.getFileName().equals("main.go")) {
                    String x = inputData.getFileName();
                    if (x.equals("")) {
                        inputData.getContentAsText();
                    }
                }

                Map<String, String> indexMap = new HashMap<>();
                try {
                    Set<ImportParser.ImportMarker> importMarkers =  new ImportParser().parse(marker.get(), inputData.getContentAsText());

                    importMarkers.forEach(importMarker -> {
                        if (importMarker.getAlias() != null) {
                            indexMap.put("a" + importMarker.getAlias(), importMarker.getProcessor());
                            indexMap.put("p" + importMarker.getAlias(), importMarker.getPkg());
                        } else {
                            indexMap.put("p" + importMarker.getProcessor(), importMarker.getPkg());
                        }
                    });
                } catch (Exception ignored) {

                }
                return indexMap;
            }
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(new FileType[]{GoFileType.INSTANCE});
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
