// Find the "Sites" menu...
var sitesMenu = widgetUtils.findObject(model.jsonModel, "id", "HEADER_SITES_MENU");
if (sitesMenu != null)
{
	// Change the widget to our custom menu for having submenu and link to document library
	sitesMenu.name = "amc/AmcSitesMenu";
}

//Remove Repository Link
//widgetUtils.deleteObjectFromArray(model.jsonModel, "id", "HEADER_REPOSITORY"); 