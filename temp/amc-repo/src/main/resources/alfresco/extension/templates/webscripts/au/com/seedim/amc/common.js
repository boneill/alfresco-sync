function checkNotNull(param){
	//check json args
	var value = json.has(param)? json.get(param) : null;
	if(!value){
		var message = "Mandatory parameter (" + param + ") is missing / invalid";
		throw message;
	}
	return value;
}

function getDirName(caseId, type){
	var retVal;
	originalCaseId = caseId;
	caseId = String(caseId);
	var length = caseId.length;
	
	if (length < 7) { // Let's mark it off at length 7
	   caseId = padWithZeros(caseId, 7 - length, true);
	   length = caseId.length;
	}
	
	if (type == '0') { //group name
	    retVal = caseId;	    
		retVal = retVal.substring(0,3);
		var toPadTo = length - retVal.length;
		retVal = padWithZeros(retVal, toPadTo, false);
	}else{ //subgroup type, 1(4-digit) & 2 (3-digit)
		var noOfDigits , range, maxEndIndex;
		//assign dir name for sub-group, based on last 3 or 4 digits
		var subGroupIndex = 0;
		var startIndex, endIndex;
		
		if(type === '1') {
			noOfDigits = 4;
			range = 1000;
			maxEndIndex = 9999;
		}else{
			noOfDigits = 3;
			range = 100;
			maxEndIndex = 999;
		}

		if(length < 3){
			//default name if case id is not value
			subGroup = "0";
		}else{
			var startPos = originalCaseId.length() < noOfDigits? 0 : (originalCaseId.length() - noOfDigits);
			subGroup = originalCaseId.substring(startPos ,originalCaseId.length());
		}
		
		if(Number(subGroup) !== 0) {// subGroup is 0
			subGroupIndex = Math.floor((Number(subGroup)-1)/range); //round downwards
		}
		
		startIndex = Number(subGroupIndex) * range + 1;
		endIndex = (Number(subGroupIndex) + 1) * range;
		endIndex = endIndex > maxEndIndex ? maxEndIndex : endIndex;
		var startIndexStr = String(startIndex);
		startIndexStr = padWithZeros(startIndexStr, (noOfDigits - startIndexStr.length), true);
		var endIndexStr = String(endIndex);

        var prefix = caseId.length > startIndexStr.length ?
            caseId.substr(0, (caseId.length - startIndexStr.length)) : "";
		startIndexStr = prefix + startIndexStr;
		endIndexStr = prefix + endIndexStr;
		retVal =  startIndexStr + "-" + endIndexStr;

	}
	return retVal;
}

function padWithZeros(initial, numberOfZeros, prefix) {
    var result = String(initial);
    for (var i = 0; i < numberOfZeros; i++) {
        if (prefix) {
            result = "0" + result;
        } else {
            result += "0";
        }
    }
    return result;
}

function getNode(nodeRefToUpdate, caseId, fileName) {
    var docNodeToUpdate;
    if (nodeRefToUpdate){
        logger.getSystem().out("Using noderef");
        docNodeToUpdate = search.findNode(nodeRefToUpdate);
        if(docNodeToUpdate== null ||docNodeToUpdate.length==0){
            var message = "Node Ref (" + nodeRefToUpdate + ") Does Not Exist";
            throw message;
        }
    }
    else{
        logger.getSystem().out("Using caseId");
        var caseNodes = search.luceneSearch("ASPECT:\"amc:candidateCase\" +@amc\\:caseId:\""+caseId+"\"");
        if(caseNodes== null ||caseNodes.length==0){
            var message = "AMC Number (" + caseId + ") Does Not Exist";
            throw message;
        }
        
        var docNodes = search.luceneSearch("PATH\\: \""+  caseNodes[0].qnamePath +"\" +@cm\\:name:\""+fileName+"\"");
        // just update the first node in the search list
        docNodeToUpdate = docNodes[0];
    }
    
    return docNodeToUpdate;
}

function updateNode(nodeRefToUpdate, caseId, fileName, description, classification) {
    var response = {};
    logger.getSystem().out("Updating node...");
    
    var docNodeToUpdate = getNode(nodeRefToUpdate, caseId, fileName);
    
    if (description) {
        logger.getSystem().out("Updating description");
        docNodeToUpdate.properties["cm:description"] = description;
    }
    
    if (classification) {
        logger.getSystem().out("Updating classification");
        docNodeToUpdate.properties["amc:candidateDocumentClassification"] = classification;
    }
    
    if (fileName) {
        logger.getSystem().out("Updating the name");
        docNodeToUpdate.name = fileName;
    }
    
    logger.getSystem().out("Saving");
    docNodeToUpdate.save();
    
    logger.getSystem().out("Packaging successful response");
    //process return result
    response.responseCode = 200;
    response.result = "success";
    response.amcNumber = caseId;
    response.nodeRef = docNodeToUpdate.nodeRef;
    return response;
}

function packageInResponseJSON(success, responseCode, responseMessage, payLoad) {
	var response = {};
	response.responseCode = responseCode;
	response.responseMessage = responseMessage;
	response.payLoad = payLoad;
	response.success = success;
	return jsonUtils.toJSONString(response);
}
