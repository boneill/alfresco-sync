<import resource="classpath:alfresco/extension/templates/webscripts/au/com/seedim/amc/common.js">
function main() {
	try{
		//parameter: docId
		var docId = url.templateArgs["docId"];
		if (!docId) {
			status.setCode(status.STATUS_NOT_FOUND, "Cannot find document for id " + docId);
			return;			
		}
		var docs = search.luceneSearch("@amc\\:amcContentID:\"" + docId + "\"");
		
		// Return 404 if it doesn't exist
		if (!docs || docs.length < 1) {
			// 404
			status.setCode(status.STATUS_NOT_FOUND, "Cannot find document for id " + docId);
			return;
		}
		var result = docs[0];
		var isSiteMember = null;
		
		var siteShortName = result.getSiteShortName();
		
		// if a valid site is returned
		if (siteShortName) {
			var currentUsername = person.properties.userName;
			var site = siteService.getSite(siteShortName);
			
			isSiteMember = site.isMember(currentUsername);   
		}
			
		var payLoad = {
			nodeRef: result.nodeRef,
			siteShortName: siteShortName,
			isSiteMember: isSiteMember
		};
			
		model.output = packageInResponseJSON(true, 200, "Successfully retrieve noderef.", payLoad);
		
	} catch(error) {
		logger.log("ERROR occurred while retrieve noderef: " + error);
		
		model.output = packageInResponseJSON(false, 500, "Fail to retrieve noderef.", error);
	}
}
main();

