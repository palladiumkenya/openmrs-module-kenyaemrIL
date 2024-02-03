<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")],
    ]

    def messageCategories = [
        [label: "Facility Referral", iconProvider: "kenyaui", icon: "", label: "CCC Referrals", href: ui.pageLink("kenyaemrIL", "referralsHome")],
        [label: "Community Referral", iconProvider: "kenyaui", icon: "", label: "Community Referrals", href: ui.pageLink("kenyaemrIL", "communityReferralsHome")]
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
.success-message-text {
    color: green;
}

.error-message-text {
    color: red;
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
            <legend>Community Referrals summary</legend>
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
                        <td> <div class="wait-loading"></div> <div class="text-wrap" align="left" id="pull-msgBox"></div></td>

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

            <div class="ke-tabmenu-item" data-tabid="completed_queue_data">Serviced referrals</div>

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
                                            <th class="dateOfBirthColumn">Referral Date</th>
                                            <th class="nupiColumn">Referred From</th>
                                            <th class="status">Status</th>
                                            <th class="action">Action</th>
                                            <th class="action">Referral Summary</th>
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
                                            <th class="dateOfBirthColumn">Referral Date</th>
                                            <th class="nupiColumn">Referred From</th>
                                            <th class="status">Status</th>
                                            <th class="action">Action</th>
                                            <th class="action">Referral Summary</th>
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

<div id="shr-dialog" title="Client Referral Details" style="display: none; background-color: white; padding: 10px;">
    <div id="shr-info">
        <div align="center" class="load-shr"></div>
        <fieldset id="show-shr-info">
            <legend>Referral Details</legend>
            <table>
                 <tr>
                    <td>Category : </td>
                    <td id="shr-category"></td>
                    <td></td>
                </tr>
                <tr>
                    <td>Referral reasons : </td>
                    <td id="shr-referral-reasons"></td>
                    <td></td>
                </tr>
                <tr>
                    <td>Clinical note : </td>
                    <td id="shr-referral-clinical-notes"></td>
                    <td></td>
                </tr>

                <table>
                    <thead>
                        <tr>
                            <td>Screening Done</td>
                            <td>Findings</td>
                        </tr>
                    </thead>
                    <tbody id="cancer_referral_data">
                    </tbody>
                </table>
            </table>
        </fieldset>
    </div>
    <div align="center">
        <button type="button" onclick="kenyaui.closeDialog();"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/cancel.png") }" /> Close</button>
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

        //SHR:Referral reasons
       // jQuery('#shr-category').text("Patient referred for medical consultation");

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
            display_loading_spinner(true, 'wait-loading');
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "pullCommunityReferralsFromFhir")}')
                   .success(function (data) {
                    if(data.status === "Success") {
                        // Hide spinner
                        display_loading_spinner(false, 'wait-loading');
                        console.log("Data ==>"+data);
                        console.log(data.message);
                        jQuery("#pull-msgBox").text(data.message + 'The page will refresh shortly');
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("success-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("error-message-text", false);

                        setTimeout(function (){
                            location.reload();
                        }, 2000);




                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false, 'wait-loading');
                        jQuery("#pull-msgBox").text(data.message);
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("success-message-text", false);


                    }
                   })
                .fail(function (err) {
                    // Hide spinner
                    display_loading_spinner(false, 'wait-loading');
                    console.log("Error fetching referral records: " + JSON.stringify(err));
                    // Hide spinner
                    //   display_loading_validate_identifier(false);
                    jQuery("#pull-msgBox").text("There was an error pulling referrals. Error: " + JSON.stringify(err));
                    jQuery("#pull-msgBox").show();
                    jQuery("#pull-msgBox").addClass("error-message-text", true);
                    jQuery("#pull-msgBox").toggleClass("success-message-text", false);

                    }
                )
        });

           jq(document).on('click','.updateButton',function(){display_loading_spinner(false, 'wait-loading');
             // Update referral_status PA
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "completeClientReferral")}',
                {
                    'patientId': jq(this).val()
                })
                .success(function (data) {
                    if(data.patientId !== " ") {
                        // Hide spinner
                        display_loading_spinner(false, 'wait-loading');
                        console.log("Successfully updated client referral: ");
                        jQuery("#pull-msgBox").text("Successfully updated client referral");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("success-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("error-message-text", false);

                        ui.navigate('kenyaemr', 'clinician/clinicianViewPatient', { patientId: data.patientId,  returnUrl: location.href });
                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false, 'wait-loading');
                        jQuery("#pull-msgBox").text("Error updating client referral");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("success-message-text", false);
                    }
                })
                .fail(function (err) {
                        // Hide spinner
                        console.log("Error updating client referral: " + JSON.stringify(err));
                        // Hide spinner
                        //   display_loading_validate_identifier(false);
                        jQuery("#pull-msgBox").text("Could not update client referral");
                        jQuery("#pull-msgBox").show();
                    jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                    jQuery("#pull-msgBox").toggleClass("success-message-text", false);

                    }
                )
        });

        jq(document).on('click','.updateSHRButton',function(){
             // Update referral_status PA
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "updateShrReferral")}',
                {
                    'patientId': jq(this).val()
                })
                .success(function (data) {
                    console.log("TEST++++ ",data)
                    if(data === "Success") {
                        // Hide spinner
                        display_loading_spinner(false, 'wait-loading');
                        console.log("Successfully updated client referra: ");
                        jQuery("#pull-msgBox").text("Successfully updated client referral");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("success-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("error-message-text", false);
                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false, 'wait-loading');
                        jQuery("#pull-msgBox").text("Error updating client referral");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("success-message-text", false);
                    }
                })
                .fail(function (err) {
                        // Hide spinner
                        console.log("Error updating client referral: " + JSON.stringify(err));
                        // Hide spinner
                        //   display_loading_validate_identifier(false);
                        jQuery("#pull-msgBox").text("Could upated client referral");
                        jQuery("#pull-msgBox").show();
                    jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                    jQuery("#pull-msgBox").toggleClass("success-message-text", false);
                    }
                )
        });
        jq(document).on('click','.viewButton',function(){
            //View referral category and reasons
            display_loading_spinner(true, 'load-shr');
            jQuery("#show-shr-info").hide();
            console.log("Am here ==>");
            // Populate referral category and reasons
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "addReferralCategoryAndReasons")}',
                {
                    'clientId': jq(this).val()
                })
                .success(function (data) {
                    if(data) {
                        display_loading_spinner(false, 'load-shr');
                        jQuery('#shr-category').text(data.category);
                        jQuery('#shr-referral-reasons').text(data.reasonCode);
                        jQuery('#shr-referral-clinical-notes').text(data.clinicalNote);

                        if(data.cancerReferral.length > 0) {
                            var referral_data_display_area = jq('#cancer_referral_data');
                            var tr;
                            referral_data_display_area.html('');
                            for (var i = 0; i < data.cancerReferral.length; i++) {

                                tr = jq('<tr/>');
                                tr.append("<td>" + data.cancerReferral[i].theTests + "</td>");
                                tr.append("<td>" + data.cancerReferral[i].theFindings + "</td>");
                                referral_data_display_area.append(tr);
                            }
                        }
                        jQuery("#show-shr-info").show();

                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false, 'wait-loading');
                        display_loading_spinner(false, 'load-shr');
                        jQuery("#pull-msgBox").text("Error updating client referral");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("success-message-text", false);
                    }
                })
                .fail(function (err) {
                        // Hide spinner
                    display_loading_spinner(false, 'load-shr');

                    console.log("Error updating client referral: " + JSON.stringify(err));
                        // Hide spinner
                        //   display_loading_validate_identifier(false);
                        jQuery("#pull-msgBox").text("Could not update client referral");
                        jQuery("#pull-msgBox").show();
                    jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                    jQuery("#pull-msgBox").toggleClass("success-message-text", false);

                    }
                )
           showReasonsFromSHR();

        });
    });   //End On ready

    function showReasonsFromSHR() {
        kenyaui.openPanelDialog({ templateId: 'shr-dialog', width: 55, height: 80, scrolling: true });
    }



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
            tr.append("<td>" + displayRecords[i].dateReferred + "</td>");
            tr.append("<td>" + displayRecords[i].referredFrom + "</td>");
            tr.append("<td>" + displayRecords[i].status +"</td>");
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'Serve client',
                class: 'updateButton',
                value: displayRecords[i].id
            });

            actionTd.append(btnView);

            tr.append(actionTd);
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'View',
                class: 'viewButton',
                id: 'show-reasons-dialog',
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
            tr.append("<td>" + displayRecords[i].dateReferred + "</td>");
            tr.append("<td>" + displayRecords[i].referredFrom + "</td>");
            tr.append("<td>" + displayRecords[i].status +"</td>");
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'Update SHR',
                class: 'updateSHRButton',
                value: displayRecords[i].id
            });

            actionTd.append(btnView);
            tr.append(actionTd);
            var actionTd = jq('<td/>');

            var btnView = jq('<button/>', {
                text: 'View',
                class: 'updateButton',
                value: displayRecords[i].id
            });

            actionTd.append(btnView);

            tr.append(actionTd);
            displayObject.append(tr);
        }
    }
    function display_loading_spinner(status, targetElementClass) {
        if(status) {
            jq("." + targetElementClass).empty();
            jq("." + targetElementClass).append(showLoadingImage);
        } else {
            jq("." + targetElementClass).empty();
        }
    }

</script>