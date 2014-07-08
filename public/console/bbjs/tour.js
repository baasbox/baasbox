	//tour
		var tours = [];
		tours["general"]=new Tour();
		tours["general"].addStep({
			element: ".brand", 
			placement: "right",
			title: "Welcome to BAASBOX", 
			content: "This is a short tour to give you a first overview of the BaasBox administrative console.<br />" +
					"At any time you can exit this tour by clicking on the 'End tour' link at the bottom of this dialog. <br />" +
					"Ready? Let's start..."
		});

		tours["general"].addStep({
			element: ".navbar-inner",
			placement: "bottom",
			title: "The Console",
			content: "This is the BaasBox Console. From this web application you can perform many administrative and configuration tasks <br>"
		});
				
		tours["general"].addStep({
			element: ".navbar-inner .btn-group",
			placement: "left",
			title: "Your Profile",
			content: "This is a quick access way to your profile. <br />" +
					 "From here you can change your password.  <br />" + 
					 "If this is the first time you access this instance, you are strongly recommended to change your default password.<br />"	
		});
		
		tours["general"].addStep({
			element: ".main-menu .hidden-tablet:contains('DB Mana')",
			placement: "right",
			title: "The main menu",
			content: "This is the main menu. There are several items grouped into categories. Each item gives access to a specific topic.<br>"
					+"The first one is 'Dashboard' which is also the home page. It gives you an overview of your instance<br>"
		});

		tours["general"].addStep({
			element: ".main-menu .hidden-tablet:contains('Help')",
			placement: "right",
			title: "Help Section",
			content: "From here you can access many useful resources. <br />" +
					"You can take this tour again, access the support site and read the online documentation. <br />"		
		});
		
		tours["general"].addStep({
			element: ".main-menu .logout",
			placement: "right",
			title: "Logout",
			content: "To exit the console, click on the 'Logout' item of the main menu.<br>"
		});		
		tours["general"].addStep({
			element: ".navbar-inner #currentVersion", 
			placement: "bottom",
			title: "That's all", 
			content: "Now what would you like to do?<br />" +
                "Check the <a href='http://www.baasbox.com/tutorial' target='_blank'>tutorial</a> section"
		});
	