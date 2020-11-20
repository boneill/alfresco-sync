<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-header.lib.js">
 
  // We need to set up a fake page structure to allow the page-centric imported function to work...
 page = { url: { templateArgs: { site: url.templateArgs.shortname}}};
 
 
 // Create a new object in the model for the pages
model.pages = [
   {
   		//add in the default dashboard page...
      label: msg.get("page.siteDashboard.title"),
      targetUrl: "site/" + url.templateArgs.shortname + "/dashboard"
   }
]

// Call the "getSitesPages" function imported from the "share-header.lib.js" file...
// Iterate over the configured pages and add the details to the model...
var pages = getSitePages();
if (pages != null)
{
   for (var i=0; i<pages.length; i++)
   {
      //Push page title and url only for Document Library Page
	   if((pages[i].pageId) == "documentlibrary" ){
		   model.pages.push({
			   label: (pages[i].sitePageTitle) ? pages[i].sitePageTitle : pages[i].title,
			   targetUrl: "site/" + url.templateArgs.shortname + "/" + pages[i].pageUrl
		   });
		   
	   }
   }
   
}

/**
// Finally push in the "site-members" page (the other default page)...
model.pages.push({
   label: msg.get("page.siteMembers.title"),
   targetUrl: "site/" + url.templateArgs.shortname + "/site-members"
});**/