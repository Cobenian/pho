# pho

An S3 File syncer with CloudFront invalidation

## Installation

Build an uberjar, copy that to pho0.2/, put pho0.2 in your path
Put your AWS access key and secret key to S3 in a file seperated by a space
## Usage

$ cd some_dir/you_wish_to_sync/
$ pho --creds="/Users/adam/.s3.cobenian" --bucket="www.demoit.io" --sync

$ echo "I changed my mind, I want to delete everything"
$ pho --creds="/Users/adam/.s3.cobenian" --bucket="www.demoit.io" --delete

$ echo "Cant remember your Cloudfront distros?"
$ pho --creds=/Users/adam/.s3.cobenian --list-distros

$ echo "we have CloudFront and need to sync and invalidate everything"
$ pho --creds=/Users/adam/.s3.cobenian --bucket="www.demoit.io" --sync  --invalidate-index="E1D3CMFUZVD34X"

## Options

None

### Bugs

Of course

## License

Copyright © 2015 Cobenian

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
