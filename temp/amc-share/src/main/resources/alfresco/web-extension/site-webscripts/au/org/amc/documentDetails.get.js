<import resource="classpath:alfresco/web-extension/site-webscripts/au/org/amc/common.js">

function main() {
	
	try {
		var redirectUrl, payLoad = {};
		
		//parameter: docId
		var docId = url.templateArgs["docId"];
		if (!docId) {
			throw {
				code: 404, 
				msg: "The item cannot be found. Either you do not have permissions to view the item, it has been removed or it never existed"
			};
		}
		
		// making a call to get the noderef and site name of the document
		var webscript = "/amc/noderef/" + docId;
		var connector = remote.connect("alfresco");
		var result = connector.get(webscript);
		
		if (result.status == 401) {
			// if fail to login to alfresco
		    // should redirect to login page and retry
			//redirectUrl = url.context + "/page?alfRedirectUrl=" + url.full;
			//redirectUrl = url.context + "/page/type/login";
			redirectUrl = url.context + "/page/";
			payLoad["redirectUrl"] = redirectUrl;
			model.output = packageInResponseJSON(false, status.code, "Redirecting to login page", payLoad);
			// redirect
			status.code = 307;  
			status.location = redirectUrl;
			return;
		} else if (result.status == 404) {
			// if failed to find the document
			throw {
				code: 404,
				msg: "The item cannot be found. Either you do not have permissions to view the item, it has been removed or it never existed"
			};
		} 
		
		var data = eval('(' + result + ')');
		if (!data || !data.payLoad || data.responseCode != "200") {
			throw {
				code: 500,
				msg: "There are some error in retrieving the document with id: " + docId
			};
		}

		if (data.payLoad.isSiteMember) {
			// navigate to the site if user is a member
			redirectUrl = url.context + "/page/site/" + data.payLoad.siteShortName + "/document-details?nodeRef=" +  data.payLoad.nodeRef;
		} else {
			// navigate to the repository browser
			redirectUrl = url.context + "/page/document-details?nodeRef=" +  data.payLoad.nodeRef;
		}
		
		payLoad["redirectUrl"] = redirectUrl;
		
		model.output = packageInResponseJSON(true, 200, "Successfully retrieve document details.", payLoad);
		
		// do redirection to the document details page
		status.code = 307; 
		status.location = redirectUrl;
		
	} catch (error) {
		logger.log("ERROR occurred in documentDetails.get.js: " + error);
		
		status.code = error.code || 500;
		status.message = error.msg || error || "ERROR occurred in documentDetails.get.js";
		status.redirect = true;
		
		model.output = packageInResponseJSON(false, status.code, status.message, status.message);
	}
	
	
}


main();
