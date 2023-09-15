#! /usr/bin/env bash
set -e

die () {
    echo >&2 "$@"
    exit 1
}

[ "$#" -eq 1 ] || die "1 argument required, $# provided"
echo $1 | grep -E -q '^[0-9]+\.[0-9]+(\.[0-9]+)$' || die "Semantic Version argument required, $1 provided"

export TAG=$1

gradle  -Pgroup=com.github.jillesvangurp -Pversion=$TAG publish

#rsync -azp  localRepo/* jillesvangurpcom@ftp.jillesvangurp.com:/srv/home/jillesvangurpcom/domains/jillesvangurp.com/htdocs/www/maven
## formation gcs repo
#gsutil -m rsync -r localRepo gs://mvn-public-tryformation/releases

echo "tagging"
git tag $TAG

echo "publishing $TAG"

git push --tags
