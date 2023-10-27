<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar" ])
    def menuItems = [
            [ label: "Back to home", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome") ]
    ]

    def messageCategories = [
        [label: "Il Messages", iconProvider: "kenyaui", icon: "", label: "Il Messages", href: ui.pageLink("kenyaemrIL", "interopManagerHome")]
    ]
%>

<div class="ke-page-sidebar">
    <div class="ke-panel-frame">
        ${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Navigation", items: menuItems ]) }
        ${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Il Admin", items: messageCategories ]) }
    </div>
</div>

<div class="ke-page-content">
    <div>
        <h2>IL Messages Page</h2>
    </div>
    <div>
        ${ ui.includeFragment("kenyaemrIL", "interopManager") }
    </div>
</div>