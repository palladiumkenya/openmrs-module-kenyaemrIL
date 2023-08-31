<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")],
    ]

    def messageCategories = [
        [label: "Facility Referral", iconProvider: "kenyaui", icon: "", label: "Facility Referral", href: ui.pageLink("kenyaemrIL", "referralsHome")],
        [label: "Community Referral", iconProvider: "kenyaui", icon: "", label: "Community Referral", href: ui.pageLink("kenyaemrIL", "communityReferralsHome")]
    ]

    ui.includeJavascript("kenyaemrorderentry", "jquery.twbsPagination.min.js")
    ui.includeJavascript("afyastat", "jsonViewer/jquery.json-editor.min.js")

    ui.includeJavascript("afyastat", "bootstrap/bootstrap.bundle.min.js")
    ui.includeCss("afyastat", "bootstrap/bootstrap-iso.css")
    ui.includeCss("afyastat", "jsonViewer/jquery.json-viewer.css")
%>
<style>
.simple-table {
    border: solid 1px #DDEEEE;
    border-collapse: collapse;
    border-spacing: 0;
    font: normal 13px Arial, sans-serif;
}
.simple-table thead th {

    border: solid 1px #DDEEEE;
    color: #336B6B;
    padding: 10px;
    text-align: left;
    text-shadow: 1px 1px 1px #fff;
}
.simple-table td {
    border: solid 1px #DDEEEE;
    color: #333;
    padding: 5px;
    text-shadow: 1px 1px 1px #fff;
}
table {
    width: 95%;
}
th, td {
    padding: 5px;
    text-align: left;
    height: 30px;
    border-bottom: 1px solid #ddd;
}
tr:nth-child(even) {background-color: #f2f2f2;}
#pager li{
    display: inline-block;
}

#active_queue-pager li{
    display: inline-block;
}

#completed-pager li{
    display: inline-block;
}
#chk-general-select-all {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
#chk-registration-select-all {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
.selectGeneralElement {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
.selectRegistrationElement {
    display: block;
    margin-left: auto;
    margin-right: auto;
}
.nameColumn {
    width: 260px;
}
.cccNumberColumn {
    width: 150px;
}
.dateRequestColumn {
    width: 120px;
}
.clientNameColumn {
    width: 120px;
}
.selectColumn {
    width: 40px;
    padding-left: 5px;
}
.actionColumn {
    width: 350px;
}
.sampleStatusColumn {
    width: 150px;
}
.sampleTypeColumn {
    width: 100px;
}

.errorColumn {
    width: 300px;
}
.pagination-sm .page-link {
    padding: .25rem .5rem;
    font-size: .875rem;
}
.page-link {
    position: relative;
    display: block;
    padding: .5rem .75rem;
    margin-left: -1px;
    line-height: 1.25;
    color: #0275d8;
    background-color: #fff;
    border: 1px solid #ddd;
}

.viewPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.viewPayloadButton:hover {
    background-color: orange;
    color: black;
}
.editPayloadButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.editPayloadButton:hover {
    background-color: orange;
    color: black;
}
.mergeButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.createButton {
    background-color: cadetblue;
    color: white;
    margin-right: 5px;
    margin-left: 5px;
}
.viewButton:hover {
    background-color: steelblue;
    color: white;
}
.mergeButton:hover {
    background-color: orange;
    color: black;
}
.createButton:hover {
    background-color: orange;
    color: black;
}
.page-content{
    background: #eee;
    display: inline-block;
    padding: 10px;
    max-width: 660px;
    font-weight: bold;
}
@media screen and (min-width: 676px) {
    .modal-dialog {
        max-width: 600px; /* New width for default modal */
    }
}
</style>

<div class="ke-page-sidebar">
    <div class="ke-panel-frame">
        ${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Navigation", items: menuItems ]) }
        ${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Referrals", items: messageCategories ]) }
    </div>
</div>

<div class="ke-page-content">

    <div>
        <fieldset>
            <legend>Referrals summary</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="15%">Total active referrals</td>
                        <td>${activeReferralListSize}</td>
                    </tr>
                    <tr>
                        <td width="15%">Total completed referrals</td>
                        <td>${completedReferralListSize}</td>
                    </tr>
                    <tr>
                        <td width="15%"> <button id="pullCommunityReferrals">Pull Community Referrals</button></td>
                        <td> <div class="wait-loading"></div> <div class="text-wrap" align="center" id="pull-msgBox"></div></td>

                       <td></td>

                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
    </div>

    <div id="program-tabs" class="ke-tabs">

        <div class="ke-tabmenu">

            <div class="ke-tabmenu-item" data-tabid="active_queue_data">Active referrals</div>

            <div class="ke-tabmenu-item" data-tabid="completed_queue_data">Completed referrals</div>

            <div class="ke-tabmenu-item" data-tabid="general_error_active_queue">Error queue</div>

        </div>

        <div class="ke-tab" data-tabid="active_queue_data">
            <table id="active_queue" cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td style="width: 99%; vertical-align: top">
                        <div class="ke-panel-frame">
                            <div class="ke-panel-heading"></div>

                            <div class="ke-panel-content">
                                <fieldset>
                                    <legend></legend>
                                    <table class="simple-table" width="100%">
                                        <thead>
                                        <tr>
                                            <th class="firstNameColumn">First Name</th>
                                            <th class="givenNameColumn">Middle Name</th>
                                            <th class="lastNameColumn">Last Name</th>
                                            <th class="genderColumn">Sex</th>
                                            <th class="dateOfBirthColumn">DOB</th>
                                            <th class="nupiColumn">NUPI</th>
                                            <th class="status">Status</th>
                                            <th class="action">Action</th>
                                        </tr>
                                        </thead>
                                        <tbody id="active_queue-list">

                                        </tbody>

                                    </table>

                                    <div id="active_queue-pager">
                                        <ul id="active_queuePagination" class="pagination-sm"></ul>
                                    </div>
                                </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>

        <div class="ke-tab" data-tabid="completed_queue_data">
            <table id="completed_queue" cellspacing="0" cellpadding="0" width="100%">
                <tr>
                    <td style="width: 99%; vertical-align: top">
                        <div class="ke-panel-frame">
                            <div class="ke-panel-heading"></div>

                            <div class="ke-panel-content">
                                <fieldset>
                                    <legend></legend>
                                    <table class="simple-table" width="100%">
                                        <thead>
                                        <tr>
                                            <th class="firstNameColumn">First Name</th>
                                            <th class="givenNameColumn">Middle Name</th>
                                            <th class="lastNameColumn">Last Name</th>
                                            <th class="genderColumn">Sex</th>
                                            <th class="dateOfBirthColumn">DOB</th>
                                            <th class="nupiColumn">NUPI</th>
                                            <th class="status">Status</th>
                                            <th class="action">Action</th>
                                        </tr>
                                        </thead>
                                        <tbody id="completed_queue-list">

                                        </tbody>

                                    </table>

                                    <div id="completed-pager">
                                        <ul id="completed_queuePagination" class="pagination-sm"></ul>
                                    </div>
                                </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</div>

<script type="text/javascript">
    var loadingImageURL = ui.resourceLink("kenyaemr", "images/loading.gif");
    var showLoadingImage = '<span style="padding:2px; display:inline-block;"> <img src="' + loadingImageURL + '" /> </span>';
    var selectedGeneralErrors = [];
    var selectedRegistrationErrors = [];
    //On ready
    jq = jQuery;
    jq(function () {

        // apply pagination for active referrals
        var active_queuePaginationDiv = jq('#active_queuePagination');
        var active_queueListDisplayArea = jq('#active_queue-list');

        var numberOfRecordsToProcess = ${ activeReferralListSize };
        var active_queueRecords = ${ activeReferralList };
        var active_queueDataDisplayRecords = [];


        var recPerPage = 10;
        var active_queueStartPage = 1;
        var totalactive_queuePages = Math.ceil(numberOfRecordsToProcess / recPerPage);

        var visibleactive_queuePages = 1;

        var payloadEditor = {};

        var sendCount = 0;


        if (totalactive_queuePages <= 5) {
            visibleactive_queuePages = totalactive_queuePages;
        } else {
            visibleactive_queuePages = 5;
        }


        if(numberOfRecordsToProcess > 0) {
            apply_pagination(active_queuePaginationDiv, active_queueListDisplayArea, totalactive_queuePages, visibleactive_queuePages, active_queueRecords, active_queueDataDisplayRecords, 'active_queue', active_queueStartPage); // records in active_queue
        }


        // apply pagination for completed referrals
        var completed_queuePaginationDiv = jq('#completed_queuePagination');
        var completed_queueListDisplayArea = jq('#completed_queue-list');

        var numberOfCompletedRecordsToProcess = ${ completedReferralListSize };
        var completed_queueRecords = ${ completedReferralList };
        var completed_queueDataDisplayRecords = [];


        var completed_queueStartPage = 1;
        var totalcompleted_queuePages = Math.ceil(numberOfCompletedRecordsToProcess / recPerPage);

        var visiblecompleted_queuePages = 1;



        if (totalcompleted_queuePages <= 5) {
            visiblecompleted_queuePages = totalcompleted_queuePages;
        } else {
            visiblecompleted_queuePages = 5;
        }


        if(numberOfCompletedRecordsToProcess > 0) {
            apply_completed_pagination(completed_queuePaginationDiv, completed_queueListDisplayArea, totalcompleted_queuePages, visiblecompleted_queuePages, completed_queueRecords, completed_queueDataDisplayRecords, 'completed_queue', completed_queueStartPage); // records in completed_queue
        }


        function apply_pagination(paginationDiv, recordsDisplayArea, totalPages, visiblePages, allRecords, recordsToDisplay, tableId, page) {
            paginationDiv.twbsPagination({
                totalPages: totalPages,
                visiblePages: visiblePages,
                onPageClick: function (event, page) {
                    displayRecordsIndex = Math.max(page - 1, 0) * recPerPage;
                    endRec = (displayRecordsIndex) + recPerPage;
                    //jq('#page-content').text('Page ' + page);
                    recordsToDisplay = allRecords.slice(displayRecordsIndex, endRec);
                    generate_table(recordsToDisplay, recordsDisplayArea, tableId);
                }
            });
        }

        function apply_completed_pagination(paginationDiv, recordsDisplayArea, totalPages, visiblePages, allRecords, recordsToDisplay, tableId, page) {
            paginationDiv.twbsPagination({
                totalPages: totalPages,
                visiblePages: visiblePages,
                onPageClick: function (event, page) {
                    displayRecordsIndex = Math.max(page - 1, 0) * recPerPage;
                    endRec = (displayRecordsIndex) + recPerPage;
                    //jq('#page-content').text('Page ' + page);
                    recordsToDisplay = allRecords.slice(displayRecordsIndex, endRec);
                    generate_completed_table(recordsToDisplay, recordsDisplayArea, tableId);
                }
            });
        }

        // handle click event of the fetch community referrals
        jq("#pullCommunityReferrals").click( function() {
            //Run the fetch tasks
            jQuery("#pull-msgBox").hide();
            console.log('Starting the fetch task!');
            // show spinner
            display_loading_spinner(true);
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "pullCommunityReferralsFromFhir")}')
                   .success(function (data) {
                    if(data.success === "true") {
                        // Hide spinner
                        display_loading_spinner(false);
                        console.log("Data ==>"+data);
                        console.log("Successfully pulled referral records: ");
                        jQuery("#pull-msgBox").text("Successfully pulled referral records");
                        jQuery("#pull-msgBox").show();
                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false);
                        jQuery("#pull-msgBox").text("Successfully pulled referral records");
                        jQuery("#pull-msgBox").show();
                    }
                   })
                .fail(function (err) {
                    // Hide spinner
                    console.log("Error fetching referral records: " + JSON.stringify(err));
                    // Hide spinner
                    //   display_loading_validate_identifier(false);
                    jQuery("#pull-msgBox").text("Successfully pulled referral records");
                    jQuery("#pull-msgBox").show();

                    }
                )
        });

           jq(document).on('click','.updateButton',function(){
            ui.navigate('kenyaemr', 'clinician/clinicianViewPatient', { patientId: jq(this).val(),  returnUrl: location.href });
             // Update referral_status PA
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "completeClientReferral")}',
                {
                    'patientId': jq(this).val()
                })
                .success(function (data) {
                    if(data.success === "true") {
                        // Hide spinner
                        display_loading_spinner(false);
                        console.log("Successfully updated client referra: ");
                        jQuery("#pull-msgBox").text("Successfully updated client referral");
                        jQuery("#pull-msgBox").show();
                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false);
                        jQuery("#pull-msgBox").text("Error updating client referral");
                        jQuery("#pull-msgBox").show();
                    }
                })
                .fail(function (err) {
                        // Hide spinner
                        console.log("Error updating client referral: " + JSON.stringify(err));
                        // Hide spinner
                        //   display_loading_validate_identifier(false);
                        jQuery("#pull-msgBox").text("Could upated client referral");
                        jQuery("#pull-msgBox").show();

                    }
                )
        });


    });

    function generate_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            tr.append("<td>" + displayRecords[i].givenName + "</td>");

            tr.append("<td>" + displayRecords[i].middleName + "</td>");
            tr.append("<td>" + displayRecords[i].familyName + "</td>");
            tr.append("<td>" + displayRecords[i].gender +"</td>");
            tr.append("<td>" + displayRecords[i].birthdate + "</td>");
            tr.append("<td>" + displayRecords[i].nupi + "</td>");
            tr.append("<td>" + displayRecords[i].status +"</td>");
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'Update client',
                class: 'updateButton',
                value: displayRecords[i].id
            });

            actionTd.append(btnView);

            tr.append(actionTd);
            displayObject.append(tr);
        }
    }

    function generate_completed_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            tr.append("<td>" + displayRecords[i].givenName + "</td>");

            tr.append("<td>" + displayRecords[i].middleName + "</td>");
            tr.append("<td>" + displayRecords[i].familyName + "</td>");
            tr.append("<td>" + displayRecords[i].gender +"</td>");
            tr.append("<td>" + displayRecords[i].birthdate + "</td>");
            tr.append("<td>" + displayRecords[i].nupi + "</td>");
            tr.append("<td>" + displayRecords[i].status +"</td>");
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'Update SHR',
                class: 'updateSHRButton',
                value: displayRecords[i].id
            });

            actionTd.append(btnView);

            tr.append(actionTd);
            displayObject.append(tr);
        }
    }
    function display_loading_spinner(status) {
        if(status) {
            jq('.wait-loading').empty();
            jq('.wait-loading').append(showLoadingImage);
        } else {
            jq('.wait-loading').empty();
        }
    }

</script>