(function()
{

	var onEmailAsLink = function(record){
		var $this = this;
		var nodeRefArr = [];
				
		if (record instanceof Array) {
			for (var i = 0, l = record.length; i < l; i++) {
				nodeRefArr.push(record[i].nodeRef);
			}
		} else {
			nodeRefArr.push(record.nodeRef);
		}
		var displayName = record.displayName;
		if (record instanceof Array) {
			displayName = "";
		}
		//mail config should be configurable in ws
		var hostName = window.location.protocol + "//" + window.location.host;
		var dataObj = {
		    nodeRefArr: JSON.stringify(nodeRefArr),
		    serverPath: hostName
		};
		Alfresco.util.Ajax.jsonRequest({
		   	method: Alfresco.util.Ajax.GET,
		   	//url: Bizpack.docminder.constants.EMAIL_CONFIG_URL,
			url:Alfresco.constants.PROXY_URI + "amc/emailink/" + nodeRefArr,
		   	//url:Alfresco.constants.PROXY_URI + "amc/emailink/" + dataObj,
		   	dataObj: dataObj,
		    responseContentType: Alfresco.util.Ajax.JSON,
		    successCallback: {
		       	fn: function onSuccessCallback(response) {
		       		//var appletValue = response.json;
		       		var execValue = response.json;
		       		//var execValue = appletValue.execValue;
		       		
		       		//NOTE: expect the mail client to be some desktop app
		       		//the current page gets redirected if mailto: protocol is to be opened ip in a web mail
		       		document.location.href = execValue;
		       	},
	            scope: $this
		    },
		    failureMessage: $this.msg("message.failure")
		});
		
	};

	YAHOO.Bubbling.fire("registerAction",
    {
        actionName: "onEmailAsLink",
        fn: onEmailAsLink
    });
	
	
	//------------------Configure Synch Action------------------------------
	
	YAHOO.Bubbling.fire("registerAction", {
    actionName: "onSetSynchMetadata",
    fn: function onSetSynchMetadata(record) {
      
      
      /**var msg = this.msg("message.synch.confirm", record.displayName)
      var rv = confirm(msg);
      if (rv === false) {
          return false;
      }**/
      this.modules.actions.genericAction({
            success: {
                events: [{
                        name: "metadataRefresh"
                    }],
                callback: {
                    fn: function DL_oAN_success(data) {
                        var resultJson = YAHOO.lang.JSON.parse(data.serverResponse.responseText);
                        //Alfresco.util.PopupManager.displayMessage({
                               // text: this.msg("message.success", record.displayName)
                            //});
                        //this.onActionDetails(record);
                        
                        //****************************************
                        //onActionDetails Method copied from action.js
                        var scope = this,
                        nodeRef = record.nodeRef,
                        jsNode = record.jsNode;
                        
                        var $html = Alfresco.util.encodeHTML,
                        $combine = Alfresco.util.combinePaths,
                        $siteURL = Alfresco.util.siteURL,
                        $isValueSet = Alfresco.util.isValueSet;
                     // Intercept before dialog show
                     var doBeforeDialogShow = function dlA_onActionDetails_doBeforeDialogShow(p_form, p_dialog)
                     {
                        // Dialog title
                        var fileSpan = '<span class="light">' + $html(record.displayName) + '</span>';

                        Alfresco.util.populateHTML(
                           [ p_dialog.id + "-dialogTitle", scope.msg("edit-details.title", fileSpan) ]
                        );

                        // Edit metadata link button
                        this.widgets.editMetadata = Alfresco.util.createYUIButton(p_dialog, "editMetadata", null,
                        {
                           type: "link",
                           label: scope.msg("edit-details.label.edit-metadata"),
                           href: $siteURL("edit-metadata?nodeRef=" + nodeRef)
                        });
                     };
                     //var templateUrl = YAHOO.lang.substitute(Alfresco.constants.URL_SERVICECONTEXT + "components/form?itemKind={itemKind}&itemId={itemId}&destination={destination}&mode={mode}&submitType={submitType}&formId={formId}&showCancelButton=true",
                     var templateUrl = YAHOO.lang.substitute(Alfresco.constants.URL_SERVICECONTEXT + "components/form?itemKind={itemKind}&itemId={itemId}&mode={mode}&submitType={submitType}&formId={formId}&showCancelButton=true",
                     {
                        itemKind: "node",
                        itemId: nodeRef,
                        mode: "edit",
                        submitType: "json",
                        formId: "synch-doclib-simple-metadata"
                     });

                     // Using Forms Service, so always create new instance
                     var editDetails = new Alfresco.module.SimpleDialog(this.id + "-editDetails-" + Alfresco.util.generateDomId());

                     editDetails.setOptions(
                     {
                        width: "auto",
                        zIndex: 1001, // This needs to be high so it works in full screen mode
                        templateUrl: templateUrl,
                        actionUrl: null,
                        destroyOnHide: true,
                        doBeforeDialogShow:
                        {
                           fn: doBeforeDialogShow,
                           scope: this
                        },
                        onSuccess:
                        {
                           fn: function dlA_onActionDetails_success(response)
                           {
                              // Reload the node's metadata
                              var webscriptPath = "components/documentlibrary/data";
                              if ($isValueSet(this.options.siteId))
                              {
                                 webscriptPath += "/site/" + encodeURIComponent(this.options.siteId)
                              }
                              Alfresco.util.Ajax.request(
                              {
                                 url: $combine(Alfresco.constants.URL_SERVICECONTEXT, webscriptPath, "/node/", jsNode.nodeRef.uri) + "?view=" + this.actionsView,
                                 successCallback:
                                 {
                                    fn: function dlA_onActionDetails_refreshSuccess(response)
                                    {
                                       var record = response.json.item
                                       record.jsNode = new Alfresco.util.Node(response.json.item.node);

                                       // Fire "renamed" event
                                       YAHOO.Bubbling.fire(record.node.isContainer ? "folderRenamed" : "fileRenamed",
                                       {
                                          file: record
                                       });

                                       // Fire "tagRefresh" event
                                       YAHOO.Bubbling.fire("tagRefresh");

                                       // Display success message
                                       Alfresco.util.PopupManager.displayMessage(
                                       {
                                          text: this.msg("message.details.success")
                                       });

                                       // Refresh the document list...
                                       this._updateDocList.call(this);
                                    },
                                    scope: this
                                 },
                                 failureCallback:
                                 {
                                    fn: function dlA_onActionDetails_refreshFailure(response)
                                    {
                                       Alfresco.util.PopupManager.displayMessage(
                                       {
                                          text: this.msg("message.details.failure")
                                       });
                                    },
                                    scope: this
                                 }
                              });
                           },
                           scope: this
                        },
                        onFailure:
                        {
                           fn: function dLA_onActionDetails_failure(response)
                           {
                              var failureMsg = this.msg("message.details.failure");
                              if (response.json && response.json.message.indexOf("Failed to persist field 'prop_cm_name'") !== -1)
                              {
                                 failureMsg = this.msg("message.details.failure.name");
                              }
                              Alfresco.util.PopupManager.displayMessage(
                              {
                                 text: failureMsg
                              });
                           },
                           scope: this
                        }
                     }).show();
                        
                        
                        //****************************************
                        

                    },
                    scope: this
                }
            },
            failure: {
                message: this.msg("message.configure.failure", record.displayName)
            },
            webscript: {
                name: "/seed/synch/configure?nodeRef="+record.nodeRef,
                stem: Alfresco.constants.PROXY_URI,
                method: Alfresco.util.Ajax.POST
            },
            config: {
                requestContentType: Alfresco.util.Ajax.JSON,
                dataObj: {
                    //nodeRefs: [file.nodeRef]
                }
            }
        });
    }
});
//----------------------Remove Synch Action--------------------------------	
	
	YAHOO.Bubbling.fire("registerAction", {
    actionName: "onRemoveSynch",
    fn: function removeSynch(record) {
        
      var msg = this.msg("message.synch.remove.confirm", record.displayName)
      var rv = confirm(msg);
      if (rv === false) {
          return false;
      }
      this.modules.actions.genericAction({
            success: {
                events: [{
                        name: "metadataRefresh"
                    }],
                callback: {
                    fn: function DL_oAN_success(data) {
                        var resultJson = YAHOO.lang.JSON.parse(data.serverResponse.responseText);
                        message: this.msg("message.unsynch.success", record.displayName)

                    },
                    scope: this
                }
            },
            failure: {
                message: this.msg("message.unsynch.failure", record.displayName)
            },
            webscript: {
                name: "/seed/synch/unsynch?nodeRef="+record.nodeRef,
                stem: Alfresco.constants.PROXY_URI,
                method: Alfresco.util.Ajax.POST
            },
            config: {
                requestContentType: Alfresco.util.Ajax.JSON,
                dataObj: {
                    //nodeRefs: [file.nodeRef]
                }
            }
        });
    }
});
	
	
	
		
})();