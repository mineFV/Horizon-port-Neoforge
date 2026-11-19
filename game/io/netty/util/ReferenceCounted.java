package io.netty.util;

/** The stand-in for Netty's ReferenceCounted: a buffer's count of holders (network buffers aren't modelled). */
public interface ReferenceCounted {
    boolean release();
}
