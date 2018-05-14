# api-draft
OGEMA-based APIs and utility bundles for app extensions, drivers and more

## Overview
The repository contains drafts, but also more mature APIs and utility bundles. As for the drafts the content
of the repository may change frequently, so this list just highlights the most important components:
* [driver-concecpts](https://github.com/smartrplace/api-draft/tree/master/src/driver-concepts-sp): API concepts for drivers that are intended to be developed in the future. APIs may
  remain in this repository, but driver implementations should be placed somewhere else.
* [example-apps](https://github.com/smartrplace/api-draft/tree/master/src/example-apps): Reference implementations that test, document and demonstrate any API in this repository. As with
  the drivers actual productive implementations of the APIs should be placed somewhere else.
* [extension-api](https://github.com/smartrplace/api-draft/tree/master/src/extension-api): APIs extending Smartrplace Apps. Currently this contains the extension API for the Smartrplace     
  Heatcontrol App. Note that extension APIs of some Smartrplace Open Source projects are published directly with
  the project (e.g.[SmartrEfficiency](https://github.com/smartrplace/smartr-efficiency)).
* [util-public](https://github.com/smartrplace/api-draft/tree/master/src/util-public): The bundle [smartrplace-util-public](https://github.com/smartrplace/api-draft/tree/master/src/util-public/smartrplace-util-public) contains several Util Classes that are used in many Smartrplace projects. Specifically it contains the following functionalities:
    - Automated table generation with the ResourceGUIHelper. There are two options for usage:
      - Setting up a ResourceEditPage that allows to choose the resource of the type specified via a dropdown. An example is given in the OGEMA tutorial in [ogema-snippets.ResourceEditPageExample](https://github.com/ogema/tutorial/blob/master/src/ogema-snippets/src/main/java/com/example/snippet/ogema/gui/ResourceEditPageExample.java).
      - Generating a table showing a set of resources of the type specified. An example is given also in [ogema-snippets.ResourceGUITablePageExample](https://github.com/ogema/tutorial/blob/master/src/ogema-snippets/src/main/java/com/example/snippet/ogema/gui/ResourceGUITablePageExample.java).A more complex and low-level example for the generation of a table via this mechnism is given in [ogema-snippets.ResourceGUITableProviderExample](https://github.com/ogema/tutorial/blob/master/src/ogema-snippets/src/main/java/com/example/snippet/ogema/gui/ResourceGUITableProviderExample.java).<br>
    - More information on the ObjectGUIHelper / ResourceGUIHelper:
      - If the page / table shall not be built based on a list of resources, but based on other objects the respective classes in package [directobjectgui](https://github.com/smartrplace/api-draft/tree/master/src/util-public/smartrplace-util-public/src/main/java/org/smartrplace/util/directobjectgui) can be used.
      - An overview on the widgets that can be generate with the GUIHelper can be obtained from the [ObjectResourceGUIHelper class](https://github.com/smartrplace/api-draft/blob/master/src/util-public/smartrplace-util-public/src/main/java/org/smartrplace/util/directobjectgui/ObjectResourceGUIHelper.java). A concept that would allow developers to extend this list is planned, but not yet available.
      - The examples above show how you can mix the simple widget generation via ResourceGUIHelper with the conventional generation of widgets that are not supported by the ResourceGUIHelper itself.
    - Methods to obtain the name of the user currently logged into the OGEMA web interface in [GUIUtilHelper](https://github.com/smartrplace/api-draft/blob/master/src/util-public/smartrplace-util-public/src/main/java/org/smartrplace/widget/extensions/GUIUtilHelper.java)
    - Utils for simple ZipFile generation and SCP file transfer in [package os/util](https://github.com/smartrplace/api-draft/tree/master/src/util-public/smartrplace-util-public/src/main/java/org/smartrplace/os/util)
  
## Build
If you have installed the [OGEMA Software Development Kit (SDK)](https://community.ogema-source.net/xwiki/bin/view/Main/)
and you have run the standard rundir you should be able to build the entire repository. For further steps see [ogema-backup-parser#Build](https://github.com/smartrplace/ogema-backup-parser#build).

## License
Apache License v2.0 ([http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0))