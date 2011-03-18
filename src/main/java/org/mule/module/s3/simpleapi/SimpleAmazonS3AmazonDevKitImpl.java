/**
 * Mule S3 Cloud Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.s3.simpleapi;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.VersionListing;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractCollection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.Validate;

public class SimpleAmazonS3AmazonDevKitImpl implements SimpleAmazonS3
{
    private final AmazonS3 s3;

    public SimpleAmazonS3AmazonDevKitImpl(@NotNull AmazonS3 s3)
    {
        Validate.notNull(s3);
        this.s3 = s3;
    }

    // 1.1
    public List<Bucket> listBuckets()
    {
        return s3.listBuckets();
    }

    // 2.1
    public Bucket createBucket(@NotNull String bucketName, String region, CannedAccessControlList acl)
    {
        Validate.notNull(bucketName);
        CreateBucketRequest request = new CreateBucketRequest(bucketName, region);
        request.setCannedAcl(acl);
        return s3.createBucket(request);
    }

    // 2.2
    public void deleteBucket(@NotNull String bucketName)
    {
        Validate.notNull(bucketName);
        s3.deleteBucket(bucketName);
    }

    public void deleteBucketAndObjects(@NotNull String bucketName)

    {
        Validate.notNull(bucketName);
        if (!s3.getBucketVersioningConfiguration(bucketName).getStatus().equals(
            BucketVersioningConfiguration.OFF))
        {
            for (S3VersionSummary summary : new S3VersionSummaryIterable(bucketName))
            {
                System.out.println(summary.getKey() + ":" + summary.getVersionId());
                s3.deleteVersion(bucketName, summary.getKey(), summary.getVersionId());
            }
        }
        else
        {
            for (S3ObjectSummary summary : listObjects(bucketName, null))
            {
                s3.deleteObject(bucketName, summary.getKey());
            }
        }
        deleteBucket(bucketName);
    }

    // 2.3
    public Iterable<S3ObjectSummary> listObjects(@NotNull String bucketName, String prefix)
    {
        Validate.notNull(bucketName);
        return new S3ObjectSummaryIterable(bucketName, prefix);
    }

    // 3.1.1
    public void deleteBucketPolicy(@NotNull String bucketName)

    {
        Validate.notNull(bucketName);
        s3.deleteBucketPolicy(bucketName);
    }

    // 3.1.2
    public String getBucketPolicy(@NotNull String bucketName)

    {
        Validate.notNull(bucketName);
        return s3.getBucketPolicy(bucketName).getPolicyText();
    }

    // 3.1.3
    public void setBucketPolicy(@NotNull String bucketName, @NotNull String policyText)

    {
        Validate.notNull(bucketName);
        Validate.notNull(policyText);
        s3.setBucketPolicy(bucketName, policyText);
    }

    // 3.2.1
    public void deleteBucketWebsiteConfiguration(@NotNull String bucketName)

    {
        Validate.notNull(bucketName);
        s3.deleteBucketWebsiteConfiguration(bucketName);
    }

    // 3.2.2
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(@NotNull String bucketName)

    {
        Validate.notNull(bucketName);
        return s3.getBucketWebsiteConfiguration(bucketName);
    }

    // 3.2.3
    public void setBucketWebsiteConfiguration(@NotNull String bucketName,
                                              @NotNull BucketWebsiteConfiguration configuration)

    {
        Validate.notNull(bucketName);
        Validate.notNull(configuration);
        Validate.notNull(configuration.getIndexDocumentSuffix());
        s3.setBucketWebsiteConfiguration(bucketName, configuration);
    }

    // 4.1
    public String createObject(@NotNull S3ObjectId objectId,
                               @NotNull S3ObjectContent content,
                               String contentType,
                               CannedAccessControlList acl,
                               StorageClass storageClass,
                               Map<String, String> userMetadata)
    {
        Validate.notNull(content);
        PutObjectRequest request = content.createPutObjectRequest();
        if (request.getMetadata() != null)
        {
            request.getMetadata().setContentType(contentType);
        }
        request.getMetadata().setUserMetadata(userMetadata);
        request.setBucketName(objectId.getBucketName());
        request.setKey(objectId.getKey());
        request.setCannedAcl(acl);
        if (storageClass != null)
        {
            request.setStorageClass(storageClass);
        }
        return s3.putObject(request).getVersionId();
    }

    // 4.2
    public void deleteObject(@NotNull S3ObjectId objectId)
    {
        Validate.notNull(objectId);
        if (objectId.isVersioned())
        {
            s3.deleteVersion(objectId.getBucketName(), objectId.getKey(), objectId.getVersionId());
        }
        else
        {
            s3.deleteObject(objectId.getBucketName(), objectId.getKey());
        }
    }

    // 4.4
    public String copyObject(@NotNull S3ObjectId source,
                             @NotNull S3ObjectId destination,
                             CannedAccessControlList acl,
                             StorageClass storageClass)
    {
        Validate.notNull(source);
        Validate.notNull(destination);
        CopyObjectRequest request = new CopyObjectRequest(source.getBucketName(), source.getKey(),
            source.getVersionId(), destination.getBucketName(), destination.getKey());
        request.setCannedAccessControlList(acl);
        if (storageClass != null)
        {
            request.setStorageClass(storageClass);
        }
        return s3.copyObject(request).getVersionId();
    }

    // 4.5
    public URI createPresignedUri(@NotNull S3ObjectId objectId, Date expiration, HttpMethod method)
    {
        Validate.notNull(objectId);
        try
        {
            return s3.generatePresignedUrl(objectId.getBucketName(), objectId.getKey(), expiration, method)
                .toURI();
        }
        catch (URISyntaxException e)
        {
            throw new AmazonClientException("S3 returned a malformed URI", e);
        }
    }

    // 4.6
    public void setObjectStorageClass(@NotNull S3ObjectId objectId, StorageClass newStorageClass)
    {
        s3.changeObjectStorageClass(objectId.getBucketName(), objectId.getKey(), newStorageClass);
    }

    // 4.3
    public InputStream getObjectContent(@NotNull S3ObjectId objectId, Date modifiedSince, Date unmodifiedSince)
    {
        Validate.notNull(objectId);
        S3Object object = getObject(objectId, modifiedSince, unmodifiedSince);
        if (object == null)
        {
            return null;
        }
        return object.getObjectContent();
    }

    @NotNull
    public ObjectMetadata getObjectMetadata(@NotNull S3ObjectId objectId)
    {
        Validate.notNull(objectId);
        return s3.getObjectMetadata(new GetObjectMetadataRequest(objectId.getBucketName(), objectId.getKey(),
            objectId.getVersionId()));
    }

    public S3Object getObject(@NotNull S3ObjectId objectId, Date modifiedSince, Date unmodifiedSince)
    {
        Validate.notNull(objectId);
        GetObjectRequest request = new GetObjectRequest(objectId.getBucketName(), objectId.getKey(),
            objectId.getVersionId());
        request.setModifiedSinceConstraint(modifiedSince);
        request.setUnmodifiedSinceConstraint(unmodifiedSince);
        return s3.getObject(request);
    }

    public void setBucketVersioningStatus(@NotNull String bucketName,
                                          @NotNull VersioningStatus versioningStatus)
    {
        Validate.notNull(bucketName);
        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName,
            new BucketVersioningConfiguration(versioningStatus.toString())));
    }

    private class S3ObjectSummaryIterable extends S3SummaryIterable<S3ObjectSummary, ObjectListing>
    {

        private String bucketName;
        private String prefix;

        public S3ObjectSummaryIterable(String bucketName, String prefix)
        {
            this.bucketName = bucketName;
            this.prefix = prefix;
        }

        @Override
        protected Iterator<S3ObjectSummary> getSummariesIterator(ObjectListing summaryListing)
        {
            return summaryListing.getObjectSummaries().iterator();
        }

        @Override
        protected boolean isTruncated(ObjectListing summaryListing)
        {
            return summaryListing.isTruncated();
        }

        @Override
        protected ObjectListing listNext(ObjectListing currentList)
        {
            return s3.listNextBatchOfObjects(currentList);
        }

        @Override
        protected ObjectListing listSummaries()
        {
            return s3.listObjects(bucketName, prefix);
        }
    }

    private class S3VersionSummaryIterable extends S3SummaryIterable<S3VersionSummary, VersionListing>
    {

        private String bucketName;

        public S3VersionSummaryIterable(String bucketName)
        {
            this.bucketName = bucketName;
        }

        @Override
        protected Iterator<S3VersionSummary> getSummariesIterator(VersionListing summaryListing)
        {
            return summaryListing.getVersionSummaries().iterator();
        }

        @Override
        protected boolean isTruncated(VersionListing summaryListing)
        {
            return summaryListing.isTruncated();
        }

        @Override
        protected VersionListing listNext(VersionListing currentList)
        {
            return s3.listNextBatchOfVersions(currentList);
        }

        @Override
        protected VersionListing listSummaries()
        {
            return s3.listVersions(bucketName, null);
        }
    }

    /**
     * Warning: this class is not a proper collection, just it implements it in order
     * to be compatible with some mule's collection splitting
     */
    private abstract class S3SummaryIterable<SummaryType, ListingType> extends
        AbstractCollection<SummaryType> implements Iterable<SummaryType>
    {

        public Iterator<SummaryType> iterator()
        {
            final ListingType summaryListing = listSummaries();
            return new Iterator<SummaryType>()
            {
                private ListingType currentList = summaryListing;
                private Iterator<SummaryType> currentIter = getSummariesIterator(summaryListing);

                public boolean hasNext()
                {
                    updateIter();
                    return currentIter.hasNext();
                }

                public SummaryType next()
                {
                    updateIter();
                    return currentIter.next();
                }

                public void remove()
                {
                    throw new UnsupportedOperationException();
                }

                private void updateIter()
                {
                    if (!currentIter.hasNext() && isTruncated(currentList))
                    {
                        currentList = listNext(currentList);
                        currentIter = getSummariesIterator(currentList);
                    }
                }
            };
        }

        protected abstract ListingType listNext(ListingType currentList);

        protected abstract boolean isTruncated(ListingType summaryListing);

        protected abstract ListingType listSummaries();

        protected abstract Iterator<SummaryType> getSummariesIterator(ListingType summaryListing);

        @Override
        public int size()
        {
            throw new UnsupportedOperationException();
        }
    }

}
