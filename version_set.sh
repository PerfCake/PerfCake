#!/bin/zsh
function updatePerfCakeVersion {

	echo "Changing scenario XSD version..."
	for f in $(find perfcake/src/*/resources/scenarios -name '*xml'); do 
		echo " - Updating file $f..."
		sed -i -e "s/urn:perfcake:scenario:.*\"/urn:perfcake:scenario:$XSD_VERSION\"/g" $f
	done

	echo "Updating Maven plugin test scenarios..."
	for f in $(find perfcake-maven-plugin/src/test/resources -name '*xml'); do 
		echo " - Updating file $f..."
		sed -i -e "s/urn:perfcake:scenario:.*\"/urn:perfcake:scenario:$XSD_VERSION\"/g" $f
	done

	echo "Updating Maven plugin test pom.xmls..."
	for f in $(find perfcake-maven-plugin/src/test/resources -name '*pom.xml'); do 
		echo " - Updating file $f..."
		sed -i -e "s/<perfcake\.version>.*<\/perfcake\.version>/<perfcake.version>$CODE_VERSION<\/perfcake.version>/g" $f
	done

	echo "Updating file perfcake/src/main/java/org/perfcake/PerfCakeConst.java..."
	sed -i -e "s/ VERSION = \".*\"/ VERSION = \"$GIT_VERSION\"/" perfcake/src/main/java/org/perfcake/PerfCakeConst.java
	sed -i -e "s/_VERSION = \".*\"/_VERSION = \"$XSD_VERSION\"/" perfcake/src/main/java/org/perfcake/PerfCakeConst.java

	echo "Updating pom.xml files..."
	for f in $(find . -maxdepth 2 -name 'pom.xml'); do 
		echo " - Updating file $f..."
		sed -i -e "s/<tag>v.*<\/tag>/<tag>v$GIT_VERSION<\/tag>/g" $f
		sed -i -e "/<parent>/,/<\/parent>/s/<version>.*<\/version>/<version>$CODE_VERSION<\/version>/" $f
	done

	echo "Updating parent pom.xml..."
	sed -i -e "1,10s/<version>.*<\/version>/<version>$CODE_VERSION<\/version>/" pom.xml

	echo "Updating bom pom.xml..."
	sed -i -e "1,10s/<version>.*<\/version>/<version>$CODE_VERSION<\/version>/" perfcake-bom/pom.xml

	echo "Updating perfcake-maven-plugin/README.md..."
	sed -i -e "s/<perfcake\.version>.*<\/perfcake\.version>/<perfcake.version>$GIT_VERSION<\/perfcake.version>/" perfcake-maven-plugin/README.md

	echo "Successfully finished!"
}

echo "===[ PerfCake source version update utility ]==="

if [ -z ${CODE_VERSION+x} ]; then 
	read CODE_VERSION\?"Enter PerfCake code version (e.g. 7.1-SNAPSHOT): "
fi
if [ -z ${XSD_VERSION+x} ]; then 
	read XSD_VERSION\?"Enter PerfCake XSD version (e.g. 7.0): "
fi
if [ -z ${GIT_VERSION+x} ]; then 
	read GIT_VERSION\?"Enter PerfCake Git tag version without prefix (e.g. 7.1): "
fi

echo "Setting the following version numbers:"
echo "Code version:    $CODE_VERSION"
echo "XSD version:     $XSD_VERSION"
echo "Git tag version: $GIT_VERSION"

echo "Do you want to proceed?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) updatePerfCakeVersion; break;;
        No ) echo "Terminating."; exit 1;;
    esac
done
