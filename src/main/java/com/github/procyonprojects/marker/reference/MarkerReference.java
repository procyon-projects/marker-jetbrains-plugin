package com.github.procyonprojects.marker.reference;

import com.goide.psi.GoResolvable;
import com.goide.psi.impl.GoReference;
import org.jetbrains.annotations.NotNull;

public class MarkerReference extends GoReference {

    public MarkerReference(@NotNull GoResolvable o) {
        super(o);
    }
}
