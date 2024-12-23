package me.myklebust.xpdoctor.validator.nodevalidator.binaryblobmissing;

import com.enonic.xp.blob.BlobKey;
import com.enonic.xp.blob.SegmentLevel;

public final class BlobReport
{
    public enum BlobState {
        MISSING,
        CORRUPTED;
    }
    SegmentLevel segmentLevel;

    BlobKey blobKey;

    BlobState state;

    public BlobReport( final SegmentLevel segmentLevel, final BlobKey blobKey, final BlobState state )
    {
        this.segmentLevel = segmentLevel;
        this.blobKey = blobKey;
        this.state = state;
    }

    @Override
    public String toString()
    {
        return blobKey + " in " + segmentLevel + " " + state;
    }
}
