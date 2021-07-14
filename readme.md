Advanced Resources Importer
===========================

A reimplementation of the Liferay Resources Importer with additional functionality and improved flexibility.

Usage
-----

At first you need to deploy the `advanced-resources-importer-service-7-x.jar`.

Depending on your Liferay version you need to deploy one additional module:

* Liferay 7.0.x: `advanced-resources-importer-components-70.jar`
* Liferay 7.1.x: `advanced-resources-importer-components-71.jar`
* Liferay 7.2.x: `advanced-resources-importer-components-72.jar`
* Liferay 7.3.x: `advanced-resources-importer-components-73.jar`
* Liferay 7.4.x: `advanced-resources-importer-components-74.jar`

To import resources you have to create a new module. This can be a JAR module and does not need to be a WAR module as in the "old" resources importer.

When using a JAR module you need to define a servlet context. This is possible by setting a `Web-ContextPath`
inside the `bnd.bnd` file.

To be detected by the Advanced Resources Importer the settings `Advanced-Resources-Importer` and `Advanced-Resources-Importer-Group` have to be added to the `bnd.bnd` file.

The setting `Advanced-Resources-Importer-Group` defines into which site the resources should be imported.
You can use `Global` for the global scope, `Guest` for the default Liferay site or the name
of any other site. If the site does not exist, the site will be created automatically.

To express that your resources need the advanced resources importer to be deployed, add a `Require-Bundle` instruction.

An example `bnd.bnd` may look like this: 

``` 
Advanced-Resources-Importer: true
Advanced-Resources-Importer-Group: Foobar
Web-ContextPath: /my-resources
Require-Bundle: de.dm.toolbox.liferay.advanced-resources-importer-service-7-x
```

Features
--------

The following features have been implemented so far:

### Creating Sites

When you provide a `Advanced-Resources-Importer-Group` that does not exist yet the site will be automatically created.

### Importing Web Content Structures and Templates

Web content structures and templates have to be present in the same directory structure as in the "old" resources importer.

The following changes were made compared to the "old" Resources Importer:

* You can rename the structures in Liferay. The names are kept as they are and are not changed when importing the resources again (if not overwritten by an entry in `assets.json`, see below)
* Web Content Templates are created as not cacheable.
* You can define a custom title, a custom description and a small image in the `assets.json` file. If a custom title or a custom description is given this will overwrite the title or description in the portal. This is done like this:

```json
{
  "assets": [
   {
      "name": "Example Structure.json",
      "title": {
        "en_US": "My custom example structure name",
        "de_DE": "Mein Titel für eine Struktur"
      },
      "description": {
        "en_US": "here comes a description"
      },
     "smallImage": "hello.png"
   }
  ]
}
```

### Importing Application Display Templates

Application Display Templates have to be present in the same directory structure as in the "old" resources importer.

The following changes were made compared to the "old" Resources Importer:

* You can rename the ADTs in Liferay. The names are kept as they are and are not changed when importing the resources again (if not overwritten by an entry in `assets.json`, see below)
* You can define a custom title, a custom description and a small image in the `assets.json` file. If a custom title or a custom description is given this will overwrite the title or description in the portal. This is done like this:

```json
{
  "assets": [
   {
      "name": "Example Template.ftl",
      "title": {
        "en_US": "My custom example template name",
        "de_DE": "Mein Titel für ein Template"
      },
     "description": {
        "en_US": "here comes a description"
      },
     "smallImage": "hello.png"
    }
  ]
}
```

For Liferay 7.3 the advanced resources importer is able to import templates for the search portlets, too.

The templates for the search portlets should be present in the following subdirectories inside `/templates-importer/templates/application_display`:

* `category_facet`: Category Facet portlet
* `custom_facet`: Custom Facet portlet
* `custom_filter`: Custom Filter portlet
* `folder_facet`: Folder Facet portlet
* `modified_facet`: Modified Facet portlet
* `search_bar`: Search Bar portlet
* `search_results`: Search Results portlet
* `site_facet`: Site Facet portlet
* `sort`: Sort portlet
* `tag_facet`: Tag Facet portlet
* `type_facet`: Type Facet portlet
* `user_facet`: User Facet portlet

### Importing Document types

Importing Document types was not possible in the "old" resources importer.

To import Document types put them into the folder `/templates-importer/document_library/document_types` as JSON files.

* You can define a custom title and a custom description in the `assets.json` file. If a custom title or a custom description is given this will overwrite the title or description in the portal. This is done like this:

```json
{
  "assets": [
   {
      "name": "Example Document Type.json",
      "title": {
        "en_US": "My custom example document type name",
        "de_DE": "Mein Titel für einen Dokumententyp"
      },
     "description": {
        "en_US": "here comes a description"
      }
    }
  ]
}
```

### Importing vocabularies and categories

Importing vocabularies and categories was not possible in the "old" resources importer.

To import vocabularies and categories, create a file `/templates-importer/vocabularies.json`.

You can define which asset types can be used for a vocabulary (selectedClassNames) and which are required fields (requiredClassNames).

For a vocabulary you can define the categories and subcategories, including additional property values.

A sample file may look like this:

```json
{
  "vocabularies": [
    {
      "name": "Event type",
      "multiValued": true,
      "selectedClassNames": [
        "com.liferay.journal.model.JournalArticle:Example Structure"
      ],
      "categories": [
        {
          "name": "Exhibition",
          "title": {
            "en_US": "Exhibition",
            "de_DE": "Ausstellung"
          },
          "description": {
            "en_US": "here comes a description"
          }
        },
        {
          "name": "Fair",
          "categories": [
            {
              "name": "Special fair"
            }
          ]
        },
        {
          "name": "Other",
          "properties": [
            { "foo":  "bar"}
          ]
        }
      ]
    }
  ]
}
```

### Documents and Media

To import Documents and Media they must be present in the same directory structure as in the "old" resources importer.

Additionally you can define a document type and metadata in the `assets.json` file.

A sample `assets.json` may look like this:

```json
{
  "assets": [
    {
      "name": "my-special-image.jpg",
      "fileEntryType": "Image with copyright",
      "fileEntryMetadata": {
        "Copyright": "Copyright by Foo"
      }
    }
  ]
}
```

### Importing Web Contents

To import Web Contents they must be present in the same directory structure as in the "old" resources importer.

Compared to the "old" Resources Importer the following changes have been made:

* Web Content Templates are being searched through the whole site hierarchy. By this you can use "Basic Webcontent", finally.
* You can refer to files in the Web Contents. Compared to the "old" Resources Importer you can provide a whole path to the files, so that you can select files in subfolder, too.

A sample file may look like this:

```xml
<?xml version="1.0"?>

<root available-locales="en_US" default-locale="en_US">
    <dynamic-element name="content" type="text_area" index-type="text" index="0">
        <dynamic-content language-id="en_US">
            <![CDATA[
			    <center>
			    <p><img alt="" src="[$FILE=footer/footer1.png$]" /></p>
			    </center>
		    ]]>
        </dynamic-content>
    </dynamic-element>
</root> 
```

### Layouts

Layouts can be imported as in the "old" resources importer using the `sitemap.json`.

Experimental support has been added to create content pages (Liferay 7.4) only. Structure is as follows:

```json
{
  "publicPages": [
    {
      "content": [
        {
          "fragmentKey": "FEATURED_CONTENT-highlights"
        },
        {
          "instanceId": "MY_CONTENT",
          "portletId": "com_liferay_journal_content_web_portlet_JournalContentPortlet",
          "portletPreferences": {
            "articleId": "MY_ARTICLE",
            "groupId": "${groupId}",
            "portletSetupPortletDecoratorId": "borderless"
          }
        }
      ]
    }
  ] 
}
```

### Tags and Categories

As in the "old" resources importer you can add tags to your content using thee `assets.json`.

Additionally can add categories to your content, too. 

An example `assets.json` may look like this

```json
{
  "assets": [
    {
      "name": "My Example Content.xml",
      "tags": [
        "logo",
        "company"
      ],
      "categories": [
        {
          "Event type": ["Exhibition", "Fair\\Special fair"]
        }
      ]
    }
  ]
}
```

Not implemented features
------------------------

The following features have not been implemented

* Creating site templates
* Creating Web Content Structures which inherit from a parent structure.
* Creating dynamic data lists

Liferay versions
----------------

This plugn has been tested against the following Liferay versions:

* Liferay CE 7.0.3 GA4
* Liferay CE 7.0.6 GA7
* Liferay CE 7.1.3 GA4
* Liferay CE 7.2.1 GA2
* Liferay CE 7.3.1 GA2
* Liferay CE 7.3.2 GA3
* Liferay CE 7.3.3 GA4
* Liferay CE 7.3.5 GA6
* Liferay CE 7.4.0 GA1
* Liferay CE 7.4.1 GA2

Developer notes
---------------

The implementation is done in a modular way. Developers can add additional functionality.

Every step of the Advanced Resources Importer is done with a component of the type `Importer.class`.
To add a new step just create a new component of this type. The order of the steps is done with the property `importer.order`.

Support for new Application Display Templates can be done with a component of type `ADT.class`.

Running the Advanced Resources Importer can be manually triggered by calling `AdvancedResourcesImporterService.importResources()`