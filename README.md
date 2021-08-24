**Alfresco Sync Addon**

The Alfresco Sync Addon allows synching content at a folder level from one site to another from Alfresco.

**Building Alfresco Sync Addon**
 
    Clone Alfresco Synch from github repo
    git clone https://github.com/boneill/alfresco-sync.git
    Build Alfresco Sync Addon Jars
    From alfresco-sync directory, run the following command 
 		   ./run.sh build_start
    The above command generates two jars located in the following directory;
   	 Repo Jar
    	alfresco-sync/alfresco-sync-platform/target/alfresco-sync-platform-1.0-SNAPSHOT.jar
	    Share Jar
 	   alfresco-sync/alfresco-sync-share/target/alfresco-sync-share-1.0-SNAPSHOT.jar

**Installing Alfresco Sync Addon**

    Installing Alfresco Jars
    This requires the installation of the Repository and Share jars.
    Download the required Jars from the release section of the alfresco-sync in Github or download the source code and build the jars as described in the 
    'Alfresco Sync Addon' section above.
    Stop Alfresco
    Copy alfresco-sync-platform-1.0-SNAPSHOT.jar to $AlfrescoWebappsHome/alfresco/WEB-INF/lib
    Copy alfresco-sync-share-1.0-SNAPSHOT.jar to $AlfrescoWebappsHome/share/WEB-INF/lib
    Start Alfresco
 
**Using Alfresco Sync Addon**
       
    CONFIGURE AND SET UP SYNCH
    The ability to configure and set up Synch is only available to Site Managers and Alfresco Administrators and the synch can only be set up at a folder level.
    
    STEPS TO SET UP SYNCH
    Hover over a folder in the Site Document Library and click More then Synch
   ![alfresco-sync-action](https://user-images.githubusercontent.com/9836573/130548361-9f2aa0d5-5a98-48a2-b49b-90c81b23e112.jpg)
    
    Click on the TargetFolder button to bring the sites folder picker
   ![alfresco-sync-form-1](https://user-images.githubusercontent.com/9836573/130548867-deeaa235-28c9-49ac-83df-509e100784a5.jpg)

    There is an additional Include subfolders option. Select this to also sync files contained in subfolders of the selected folder. Already selected by default.
   
    Choose the Desired folder in a site and hit OK. Then Save.
    There is an indicator to show that this folder is a Sync Folder.
   ![alfresco-synch-indicator-1](https://user-images.githubusercontent.com/9836573/130549101-43558306-37f5-4738-b2a3-945283e7e3b6.jpg)
   
    SYNCH SOURCE FOLDER DETAILS
    In order to view the Synch Source Folder Details, hover over the Synch Source folder in and click More then View Details.
    Under the Properties Section, the Synch information is displayed.
   ![alfresco-synch-source-metadata](https://user-images.githubusercontent.com/9836573/130549274-f79abe44-a290-4dea-9f95-8703000468ac.jpg)
   
    You can click on the link besides the Synch Target Parent Folder to get to the destination synch target parent folder containing the actual synched folder. 
    You can use the same mechanism from the Synch Target Parent Folder to get to the source synch target parent folder.
    
    REMOVING THE SYNCH
    The synch can be terminated by using the UnSynch Action available on the Synched Source Folder
    Hover over the Synch Source folder in and click More then UnSynch.
   ![alfresco-synch-unsynch-action](https://user-images.githubusercontent.com/9836573/130549575-7569476d-3a65-400e-b103-207fefacccb0.jpg)

    FAILED SYNCH DETAILS
    Whenever the synch job fails for a particular reason, a synch failed indicator is shown on the Synch Source Folder.
   ![alfresco-synch-indicator-2](https://user-images.githubusercontent.com/9836573/130549657-3854375b-88b6-4ba3-8cf0-18c39ad882e7.jpg)
    
    Besides the synch failed indicator, the error time and error details are displayed on the Synch Source Folder details page. 
    In this case, there is a file with the same name already in the Synch Target Folder.
   ![alfresco-sync-error](https://user-images.githubusercontent.com/9836573/130550100-cde6d878-f43c-4b1d-8bb2-19f0b6e19e21.jpg)

    RESET FAILED SYNCH
    When there is a failed synch, it is expected that the site manager takes the appropriate action based on the error and then click on the Reset Synch action.
    So to remediate the issue of above synch error, the site manager must rename or delete the file in the Synch Target Folder and then click Reset Failed Synch.
   ![alfresco-synch-reset-synch-action](https://user-images.githubusercontent.com/9836573/130550207-41299087-554a-4bc1-b44d-6db1503735ff.jpg)

    MISCELLANEOUS NOTES
    The frequency at which the synch job runs every 30 seconds but it can be adjusted through the following config in alfreso-global.properties
    synch.scheduledjob.cronexpression=0/30 * * * * ?
    The synch job is run as the Admin user by default but it can be adjusted through the following config in alfreso-global.properties
    synch.scheduledjob.runasusername=admin
    The site manager or admin user must ensure that the correct Synch Target Parent is selected or else the latter will need to Unsynch and Synch again.
    Only the Name and Title properties are being tracked/monitored for changes in the Sync Source and replicated to the Sync Target






