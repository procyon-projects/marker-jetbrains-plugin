package com.github.procyonprojects.marker.index;

import com.github.procyonprojects.marker.lang.MarkerProcessorsType;
import com.github.procyonprojects.marker.metadata.provider.MetadataProvider;
import com.goide.GoFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class MarkerProcessorPackagesIndex extends FileBasedIndexExtension<String, String> {

    public static final MetadataProvider METADATA_PROVIDER = ApplicationManager.getApplication().getService(MetadataProvider.class);
    public static final ID<String, String> KEY = ID.create("go.marker.packages");

    @Override
    public @NotNull ID<String, String> getName() {
        return KEY;
    }

    @Override
    public @NotNull DataIndexer<String, String, FileContent> getIndexer() {
        return (inputData) -> {
            String processorPackageFullPath = FilenameUtils.getPathNoEndSeparator(inputData.getFile().getPath());
            int lastIndexOfSeparator = FilenameUtils.indexOfLastSeparator(processorPackageFullPath);
            int indexOfMarkerBasePath = processorPackageFullPath.indexOf("marker");
            String processorPackage = processorPackageFullPath.substring(indexOfMarkerBasePath + "marker".length() + 1, lastIndexOfSeparator);
            String version = processorPackageFullPath.substring(lastIndexOfSeparator + 1);

            String pkg = processorPackage + "@" + version;
            if (METADATA_PROVIDER.packageExistsInCache(pkg)) {
                METADATA_PROVIDER.updateCache(pkg, String.valueOf(inputData.getContentAsText()));
            }

            return Collections.singletonMap(pkg, String.valueOf(inputData.getContentAsText()));
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
        return new DefaultFileTypeSpecificInputFilter(new FileType[]{MarkerProcessorsType.INSTANCE});
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
