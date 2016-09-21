#!/bin/bash
function updateModel {
	mkdir tmp
	cd tmp
	xjc -p org.perfcake.model ../perfcake/src/main/resources/schemas/perfcake-scenario-${XSD_VERSION}.xsd
	for i in org/perfcake/model/*; do
		cat ../license-header.txt $i > ../perfcake/src/main/java/$i
	done
	cd ..
	rm -rf tmp
}

echo "===[ Update XSD model classes ]==="

if [ -z ${XSD_VERSION+x} ]; then
        read -e -p "Enter XSD version (e.g. 7.0): " XSD_VERSION
fi

echo "Updating classes in org.perfcake.model according to perfcake-scenario-${XSD_VERSION}.xml"
echo "Do you want to proceed?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) updateModel; break;;
        No ) echo "Terminating."; exit 1;;
    esac
done
