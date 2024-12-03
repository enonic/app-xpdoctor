package me.myklebust.xpdoctor.filespy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.enonic.xp.blob.BlobStore;
import com.enonic.xp.blob.BlobStoreProvider;

@Component(immediate = true, service = FileBlobStoreSpyService.class)
public class FileBlobStoreSpyService
{
    private volatile BlobStore blobStore;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addBlobStoreProvider( final BlobStoreProvider provider )
    {
        if ( provider.name().equals( "file" ) )
        {

            this.blobStore = provider.get();
        }
    }

    public void removeBlobStoreProvider( final BlobStoreProvider provider )
    {
        // ignore
    }

    public BlobStore getBlobStore()
    {
        return this.blobStore;
    }
}
