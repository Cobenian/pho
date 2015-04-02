# pho

An S3 File syncer

## Installation

Build an uberjar, copy that to pho0.1/, put pho0.1 in your path
Put your AWS access key an secret key to S3 in a file seperated by a space
## Usage

$ cd some_dir/you_wish_to_sink/
$ pho --creds="/Users/adam/.s3.cobenian" --bucket="www.demoit.io" --sync
$ echo "I changed my mind, I want to delete everything"
$ pho --creds="/Users/adam/.s3.cobenian" --bucket="www.demoit.io" --delete

## Options

None 

### Bugs

Of course 

## License

Copyright © 2015 Cobenian 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
