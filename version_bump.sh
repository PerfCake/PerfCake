#!/bin/sh
VERSION=$(cat perfcake/src/main/java/org/perfcake/PerfCakeConst.java | grep ' VERSION =' | sed 's/^.* = "//' | sed 's/\..*$//')
NEXT=$(( $VERSION + 1 ))

function bumpPerfCake {
	echo "Bumping from version $VERSION to version $NEXT"

	for f in $(find perfcake/src/*/resources/scenarios -name '*xml'); do 
		echo "Updating file $f..."
		sed -i -e "s/urn:perfcake:scenario:$VERSION\.0/urn:perfcake:scenario:$NEXT.0/g" $f
	done

	echo "Updating file perfcake/src/main/java/org/perfcake/PerfCakeConst.java..."
	sed -i -e "s/VERSION = \"$VERSION\../VERSION = \"$NEXT.0/" perfcake/src/main/java/org/perfcake/PerfCakeConst.java

	for f in $(find . -maxdepth 2 -name 'pom.xml'); do 
		echo "Updating file $f..."
		sed -i -e "s/<tag>v$VERSION\../<tag>v$NEXT.0/g" $f
	done

	echo "Successfully finished!"
}

echo "Do you wish to bump PerfCake major version from $VERSION to $NEXT?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) bumpPerfCake; break;;
        No ) echo "Terminating."; exit 1;;
    esac
done
