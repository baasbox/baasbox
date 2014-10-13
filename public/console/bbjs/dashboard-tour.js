	//tour
		var tours = [];
		tours["general"]=new Tour();
		generalTour.addStep({
			element: ".brand", 
			placement: "right",
			title: "Welcome to BAASBOX", 
			content: "This is a short tour to give you a first overview of the BaasBox administrative console.<br /> "  
						+"At any time you can exit from this tour by clicking on the 'End tour' link at the bottom of this dialog. <br />"
						+"Ready? Let's start...<br>"
		});

		generalTour.addStep({
			element: ".navbar-inner",
			placement: "bottom",
			title: "The Console",
			content: "This is the BaasBox Console. From this web application you can perform many administrative and configuration tasks <br>"
		});
				
		generalTour.addStep({
			element: ".navbar-inner .btn-group",
			placement: "left",
			title: "Your profile",
			content: "This is a quick access way to your profile.<br>From here you can change your password.<br>If this is the first time you access to this instance,"
					+" you are strongly encouraged to change your default password.<br>"
		});
		
		generalTour.addStep({
			element: ".main-menu .hidden-tablet:contains('DB Mana')",
			placement: "right",
			title: "The main menu",
			content: "This is the main menu. There are several items grouped into categories. Each item gives access to a specific topic.<br>"
					+"The first one is 'Dashboard' which is also the home page. It gives you an overview of your instance<br>"
		});

		generalTour.addStep({
			element: ".main-menu .hidden-tablet:contains('Help')",
			placement: "right",
			title: "Help section",
			content: "From here you can access to many useful resources.<br>"
					+"You can repeat this tour, make a short one of the current selected page, access to the support site and read the online documentation.<br>"
		});
		generalTour.addStep({
			element: ".main-menu .logout",
			placement: "right",
			title: "Logout",
			content: "To exit from the console, click the 'Logout' item of the main menu.<br>"
		});		
		generalTour.addStep({
			element: ".navbar-inner #currentVersion", 
			placement: "bottom",
			title: "That's all", 
			content: "This rapid tour is finished. Now you are encouraged to explore each item of the menu, see the related tours, read the online documentation and build your first BAASBOX powered App.<br>"
		});
	