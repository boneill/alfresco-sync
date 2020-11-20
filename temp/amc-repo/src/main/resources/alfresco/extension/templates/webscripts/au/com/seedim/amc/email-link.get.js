/**
 * Pre-condition:
 * Email Client is setup in client machine
 * 
 * Windows Commands
 * */

//NOTE: the domain url MUST be changed to a valid one if this should be used, e.g. "http://192.168.2.47:8080/share/page/";
//Leave it to null, if serverPath will always be supplied
var SHARE_DOMAIN = null;
var SUBJECT_TEMPLATE = "Linked Documents";
/* NOTE: for url to be rendered correctly as a clickable link by email client, 
 * leave a space before url and after to indicate the end of a link */
var BODY_TEMPLATE = "Link: \n {url}";
var LINK_TO_FOLDER_DETAILS = "/share/page/folder-details?nodeRef=";
var LINK_TO_DOCUMENT_DETAILS = "/share/page/document-details?nodeRef=";


function main() {

	//arguments & variables	
	var nodeRefStr = args["nodeRefArr"];
	SHARE_DOMAIN = SHARE_DOMAIN || args["serverPath"] || "";
	
	var nodeRefArr = eval("(" + nodeRefStr + ")");
	
	var subjectText = ""; 
	var bodyText = "";
	

	for (var i = 0, l = nodeRefArr.length; i < l; i++) {
		// based on the nodeRef, search for the node
		var node = search.findNode(nodeRefArr[i]);
		
		if (node) {
			// prepare the subject and body
			bodyText += prepareBody(node);
		}
	}
	
	// replace the value in template
	var mailSubject = SUBJECT_TEMPLATE;
	var mailBody = BODY_TEMPLATE.replace(/{url}/g, bodyText);
	
	// encode the text
	var encodedSubject = stringUtils.urlEncode(mailSubject);
	var encodedBody = stringUtils.urlEncode(mailBody);

	var mailConfig = "?subject=" + encodedSubject + "&body=" + encodedBody;
	var execValue = "mailto:" + mailConfig;
	
	//model.execValue = jsonUtils.encodeJSONString(execValue);
	model.execValue = jsonUtils.toJSONString(execValue);;
}


function prepareBody(node) {
	var amcDocumentId = node.properties["amc:amcContentID"];
	var documentDetailsLink = "/share/page/service/details/" + amcDocumentId; 
	var bodyText = "Name: " + node.name + " \n ";
	var siteName = "";
	if(node.isDocument){
		if(amcDocumentId != null){
			bodyText += SHARE_DOMAIN + documentDetailsLink + " \n\n ";
		}else{
			bodyText += SHARE_DOMAIN + LINK_TO_DOCUMENT_DETAILS + node.nodeRef + " \n\n ";
		}
	}
	if(node.isContainer){
		if ((node.displayPath).indexOf("Sites")>= 0){
			 var nodePath = node.displayPath;
			 var nodePathArray = nodePath.split("/");
			 siteName = nodePathArray[3];
			 bodyText += SHARE_DOMAIN + "/share/page/site/" + siteName + "/folder-details?nodeRef=" + node.nodeRef + " \n\n ";
			  
		}else{
			bodyText += SHARE_DOMAIN + LINK_TO_FOLDER_DETAILS + node.nodeRef + " \n\n ";	
		}
		
	}	
//bodyText += SHARE_DOMAIN + (node.isDocument ? (amcDocumentId ? documentDetailsLink : LINK_TO_DOCUMENT_DETAILS + node.nodeRef) : LINK_TO_FOLDER_DETAILS + node.nodeRef) + " \n\n ";

	return bodyText;
}

main();
