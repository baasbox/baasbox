		tours["whatsnew"]=new Tour();

		tours["whatsnew"].addStep({
			element: ".navbar-inner",
			placement: "bottom",
			title: "What's new",
			content: "In this short tour you will see the news and the main differences between this version and the previous one"
		});
		
		tours["whatsnew"].addStep({
			element: ".hidden-tablet:contains('DB')",
			placement: "right",
			title: "Database management",
			content: "This new menu item allows you to perform several tasks on the embedded database. You can now perform backups and store them on the BaasBox server, list all the stored backup, download and restore them.<br />"
					+"You can even reinitialize the database and restart from scratch."
		});

		tours["whatsnew"].addStep({
			element: ".main-menu .hidden-tablet:contains('Help')",
			placement: "right",
			title: "Help section",
			content: "We added the short tours and the links to useful online resources, like the documentation and the support site.<br />"
					+"Furthermore each page and element has now a brief explanation of its purpose."
		});
		tours["whatsnew"].addStep({
			element: ".main-menu .hidden-tablet:contains('Collections')",
			placement: "right",
			title: "Drop a collection and more details",
			content: "In the Collections list now further details are available: the number of each document belonging to the collection. Furthermore now it is possible to drop an entire collection ad delete each single document inside it with a single command."
		});		
		tours["whatsnew"].addStep({
			element: ".main-menu .hidden-tablet:contains('Documents')", 
			placement: "right",
			title: "New columns on the list of documents", 
			content: "We deprecated the RID, so now it is no longer displayed on the list of document. The ID field is now shown.<br>"
		});
	