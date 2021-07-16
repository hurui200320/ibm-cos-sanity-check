# ibm-cos-sanity-check
A tool for calculating SHA3-256 of each file in a given bucket.

Still Working In Progress, but should be functional.

## Usage

Deploy [the docker](https://hub.docker.com/r/hurui200320/ibm-cos-sanity-check) image on IBM Cloud Code Engine as `Job`, set some environment variables, then let the job run. You can set up a IBM Log Analysis to see if anything goes wrong. The program will list all sub directories and calculate SHA3-256 of each file listed. The result will be stored in file per folder, contains all file's checksum in that folder.

For example, I have a folder:

+ some_prefix/
  + subfolder1/
    + files_in_subfolder_1...
  + subfolder2/
    + files_in_subfolder_2
  + files_in_folder...

Each file in current folder (`files_in_folder`) will be calculate, and the results will be stored at `some_prefix/SANITY_CHECK.txt`, and `files_in_subfolder1`'s checksum will be stored at `some_prefix/subfolder1/SANITY_CHECK.txt`, `files_in_subfolder2`'s checksum will be stored at `some_prefix/subfolder2/SANITY_CHECK.txt`.

The format of `SANITY_CHECK.txt` is:

```
# Prefix: /<YOUR_PREFIX>
SHA3-256:<SHA_RESULT>:<OBJECT_KEY>
ERROR:<ERROR_MESSAGE>:<OBJECT_KEY>
...
```

The first line start with `#` and it gives the prefix of current folder.

Start from the second line, each line gives `<FORMAT>:<PAYLOAD>:<OBJECT_KEY>`, `<FORMAT>` has three options: `SHA3-256`, `ERROR` and `DEBUG`. For `SHA3-256`, `<PAYLOAD>` gives the SHA3-256 result of object. `ERROR` means something goes wrong when calculating the hash, and the `<PAYLOAD>` gives the error message. For `DEBUG`, the payload is always `SKIP`.

If there are something wrong during the listing file or initialization stage, please check the log, there should be a ERROR message.

## Environment variables

### `IAM_ENDPOINT`

Set the IBM IAM endpoint URL, this is not required, the default value is `https://iam.cloud.ibm.com/identity/token`.

### `COS_ENDPOINT`

**Required.** This is the S3 endpoint of your IBM COS. For more details, please refer to [this document](https://cloud.ibm.com/docs/cloud-object-storage?topic=cloud-object-storage-endpoints#endpoints-region).

Please do notice, when running this docker in Code Engine, **please use Direct endpoint for free bandwidth, use public endpoint will cause public endpoint fee.**

### `COS_APIKEY`

**Required.** This is your API key.

### `COS_SERVICE_CRN`

**Required.** This is your service CRN, you can find it in your access credential.

### `COS_BUCKET_LOCATION`

**Required.** Though it is not used, since this parameter is only used when doing HMAC things, but create COS client require this parameter.

### `COS_BUCKET_NAME`

**Required.** This is your COS bucket name.

### `APP_PREFIX`

**Required.** This is the prefix. You cannot set it to empty string in IBM Cloud Code Engine console, also I don't recommend running this docker with root folder, since I cannot make sure my code will function correctly. So try to limit the scope by using the prefix.

### `APP_SANITY_NAME`

This is the result file name. This is not required, the default value is `SANITY_CHECK.txt`.

### `APP_DEBUG`

This parameter will put program in debug mode. In debug mode, the calculation will not performed, thus it always gives `SKIP` as result. Also, in debug mode, the result will not be write into the bucket.

## TODOs

Currently the program cannot distinguish different storage class. From the documentation, standard class will return null when trying to get the storage class. So only object which `storageClass == null` will be calculated, rest of them will gives error.

Also, I want to implement some checking when some files' checksum is represented in the sanity check file, but considering some files might in archived mode, I have no idea of how to do this.

Last but not least, this is only my side project, I moved my recently (in past 3 years) unused freezing cold personal data from Google drive to IBM Cloud COS, by using the archived storage class, I can get the price as low as 1.02USD per TB per month, and also free myself from maintaining a bunch of offline hard drives or online NAS in my house. The tape drive is too expensive for me, so I guess this is the best plan.

Feel free to start a discussion, submit an issue, or create a pull request.
