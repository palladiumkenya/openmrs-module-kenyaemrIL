<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")],
    ]

    def messageCategories = [
        [label: "Facility Referral", iconProvider: "kenyaui", icon: "", label: "CCC Referrals", href: ui.pageLink("kenyaemrIL", "referralsHome")]
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

#queue-pager li{
    display: inline-block;
}

#archive-pager li{
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
            <legend>Facility Referrals summary</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="15%">Total Expected Transfer Ins</td>
                        <td>${queueListSize}</td>
                    </tr>
                    <tr>
                        <td width="15%">Total Completed Referrals</td>
                        <td>${archiveListSize}</td>
                    </tr>
                    <tr>
                        <td width="15%">Total errors</td>
                        <td>0</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
    </div>
    <fieldset>
        <div class="text-wrap" align="left" id="pull-msgBox"></div>
    </fieldset>

    <div id="program-tabs" class="ke-tabs">

        <div class="ke-tabmenu">

            <div class="ke-tabmenu-item" data-tabid="queue_data">Pending Referrals</div>

            <div class="ke-tabmenu-item" data-tabid="archive_data">Completed Referrals</div>

        </div>

        <div class="ke-tab" data-tabid="queue_data">
            <table id="queue" cellspacing="0" cellpadding="0" width="100%">
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
                                            <th class="clientNameColumn">Patient Identifier</th>
                                            <th class="sampleTypeColumn">UPI</th>
                                            <th class="cccNumberColumn">Patient Name</th>
                                            <th class="dateRequestColumn">TransferOut Date</th>
                                            <th class="dateRequestColumn">Appointment Date</th>
                                            <th class="dateRequestColumn">Transfer In Date</th>
                                            <th class="action">Action</th>
                                        </tr>
                                        </thead>
                                        <tbody id="queue-list">

                                        </tbody>

                                    </table>

                                    <div id="queue-pager">
                                        <ul id="queuePagination" class="pagination-sm"></ul>
                                    </div>
                                </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>

        <div class="ke-tab" data-tabid="archive_data">
            <table id="archive" cellspacing="0" cellpadding="0" width="100%">
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
                                            <th class="clientNameColumn">Patient Identifier</th>
                                            <th class="sampleTypeColumn">UPI</th>
                                            <th class="cccNumberColumn">Patient Name</th>
                                            <th class="dateRequestColumn">TransferOut Date</th>
                                            <th class="dateRequestColumn">Appointment Date</th>
                                            <th class="dateRequestColumn">TO Acceptance Date</th>
                                        </tr>
                                        </thead>
                                        <tbody id="archive-list">

                                        </tbody>

                                    </table>

                                    <div id="archive-pager">
                                        <ul id="archivePagination" class="pagination-sm"></ul>
                                    </div>
                                </fieldset>
                            </div>
                        </div>
                    </td>
                </tr>
            </table>
        </div>
    </div>

    <div class="bootstrap-iso">

        <div class="modal fade" id="showViewPayloadDialog" tabindex="-1" role="dialog" aria-labelledby="backdropLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-xl" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="backdropLabel">View Payload</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-view-display"></pre>
                    </div>
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="showEditPayloadDialog" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" role="dialog" aria-labelledby="staticBackdropLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-xl" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="staticBackdropLabel">Edit Payload</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-edit-display"></pre>
                    </div>
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="savePayloadButton btn btn-primary">Save and Requeue</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal fade" id="showConfirmationBox" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" role="dialog" aria-labelledby="staticConfirmLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-sm" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="staticConfirmLabel">Please Confirm</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-confirm-display"></pre>
                    </div>
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="confirmNoButton btn btn-secondary" data-bs-dismiss="modal">No</button>
                        <button type="button" class="confirmYesButton btn btn-primary">Yes</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal" id="showWaitBox" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" role="dialog" aria-labelledby="staticConfirmLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-sm" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="staticConfirmLabel">Please Wait</h5>
                    </div>
                    <div class="modal-body">
                        <div>
                            <span style="padding:2px; display:inline-block;"> <img src="${ui.resourceLink("afyastat", "images/loading.gif")}" /> </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="modal" id="showInfoBox" data-bs-backdrop="static" data-bs-keyboard="false" tabindex="-1" role="dialog" aria-labelledby="staticInfoLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-sm" role="document">
                <div class="modal-content">
                    <div class="modal-header modal-header-primary">
                        <h5 class="modal-title" id="staticInfoLabel">Info</h5>
                    </div>
                    <div class="modal-body">
                        <span style="color: firebrick" id="msgBox"></span>
                        <pre id="json-info-display"></pre>
                    </div>
                    <div class="modal-footer modal-footer-primary">
                        <button type="button" class="confirmOkButton btn btn-secondary" data-bs-dismiss="modal">OK</button>
                    </div>
                </div>
            </div>
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
        // apply pagination

        var generalErrorPaginationDiv = jq('#generalErrorPagination');
        var queuePaginationDiv = jq('#queuePagination');
        var archivePaginationDiv = jq('#archivePagination');

        var queueListDisplayArea = jq('#queue-list');
        var archiveListDisplayArea = jq('#archive-list');

        var numberOfRecordsToProcess = ${ queueListSize };
        var numberOfArchivesToProcess = ${ archiveListSize };

        var queueRecords = ${ queueList };
        var archiveRecords = ${ archiveList };

        var generalErrorDataDisplayRecords = [];
        var queueDataDisplayRecords = [];
        var archiveDataDisplayRecords = [];

        var recPerPage = 10;

        var generalErrorStartPage = 1;
        var queueStartPage = 1;
        var archiveStartPage = 1;

        var totalQueuePages = Math.ceil(numberOfRecordsToProcess / recPerPage);
        var totalArchivePages = Math.ceil(numberOfArchivesToProcess / recPerPage);

        var visibleGeneralErrorPages = 1;
        var visibleQueuePages = 1;
        var visibleArchivePages = 1;

        var payloadEditor = {};

        var sendCount = 0;

        if (totalArchivePages <= 5) {
            visibleArchivePages = totalArchivePages;
        } else {
            visibleArchivePages = 5;
        }

        if (totalQueuePages <= 5) {
            visibleQueuePages = totalQueuePages;
        } else {
            visibleQueuePages = 5;
        }

        if(numberOfRecordsToProcess > 0) {
            apply_pagination(queuePaginationDiv, queueListDisplayArea, totalQueuePages, visibleQueuePages, queueRecords, queueDataDisplayRecords, 'queue', queueStartPage); // records in queue
        }

        if (numberOfArchivesToProcess > 0) {
            apply_pagination(archivePaginationDiv, archiveListDisplayArea, totalArchivePages, visibleArchivePages, archiveRecords, archiveDataDisplayRecords, 'archive', archiveStartPage); // archives
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

        function AsyncConfirmYesNo(title, msg, yesFn, noFn) {
            jq("#staticConfirmLabel").html(title);
            jq("#json-confirm-display").html(msg);
            jq(".confirmYesButton").off('click').click(function () {
                yesFn();
                jq('#showConfirmationBox').modal("hide");
            });
            jq(".confirmNoButton").off('click').click(function () {
                noFn();
                jq('#showConfirmationBox').modal("hide");
            });
            jq('#showConfirmationBox').modal('show');
        }

        function AsyncShowInfo(title, msg, okFn) {
            jq("#staticInfoLabel").html(title);
            jq("#json-info-display").html(msg);
            jq(".confirmOkButton").off('click').click(function () {
                okFn();
                jq('#showInfoBox').modal("hide");
            });
            jq('#showInfoBox').modal('show');
        }

        function reloadPage() {
            document.location.reload();
        }

       jq(document).on('click','.openPatient',function(){
         //Register and open patient
           display_loading_spinner(true);
            jQuery.getJSON('${ ui.actionLink("kenyaemrIL", "referralsDataExchange", "artReferralsHandler")}',
                {
                    'patientId': jq(this).val()
                })
                .success(function (data) {
                    if(data.patientId !== " ") {
                        // Hide spinner

                        display_loading_spinner(false);
                        console.log("Successfully registered patient");
                        jQuery("#pull-msgBox").text("Successfully registered patient");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("success-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("error-message-text", false);
                        setTimeout(function () {
                            if(data.isClinician) {
                                ui.navigate('kenyaemr', 'clinician/clinicianViewPatient', { patientId: data.patientId,  returnUrl: location.href });
                            } else {
                                ui.navigate('kenyaemr', 'registration/registrationViewPatient', { patientId: data.patientId,  returnUrl: location.href });
                            }
                        },2000)
                    }else{
                        console.log("Data ==>"+data);
                        display_loading_spinner(false);
                        jQuery("#pull-msgBox").text("Error registering client");
                        jQuery("#pull-msgBox").show();
                        jQuery("#pull-msgBox").toggleClass("error-message-text", true);
                        jQuery("#pull-msgBox").toggleClass("success-message-text", false);
                    }
                })
                .fail(function (err) {
                        // Hide spinner
                        console.log("Error registering client: " + JSON.stringify(err));
                        // Hide spinner
                        //   display_loading_validate_identifier(false);
                        jQuery("#pull-msgBox").text("Could update client referral");
                        jQuery("#pull-msgBox").show();

                    }
                )
       });

        jq(document).on('click','.mergeButton',function(){
            ui.navigate('afyastat', 'mergePatients', { queueUuid: jq(this).val(),  returnUrl: location.href });
        });

        jq(document).on('click','.viewPayloadButton',function () {
            var queueUuid = jq(this).val();
            console.log("Checking for queue entry with uuid: " + queueUuid);

            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'getMessagePayload', { queueUuid : queueUuid }, function (result) {
                let payloadObject = [];
                try {
                    payloadObject = JSON.parse(result.payload);
                } catch(ex) {
                    payloadObject = JSON.parse("{}")
                }
                
                jq('#json-view-display').empty();
                jq('#json-view-display').jsonViewer(payloadObject,{
                    withQuotes:true,
                    rootCollapsable:true
                });
            });

            jq('#showViewPayloadDialog').modal('show');
        });


        // used to create new registration and bypass any patient matching on the provided patient demographics
        jq(document).on('click','.createButton',function () {
            var queueUuid = jq(this).val();
            ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'createNewRegistration', { queueUuid : queueUuid }, function (result) {
                document.location.reload();
            });
        });
    });

    function generate_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            tr.append("<td>" + displayRecords[i].cccNumber + "</td>");
            tr.append("<td>" + displayRecords[i].upiNumber + "</td>");
            tr.append("<td>" + displayRecords[i].patientName + "</td>");
            tr.append("<td>" + displayRecords[i].transferOutDate + "</td>");
            tr.append("<td>" + displayRecords[i].appointmentDate + "</td>");
            tr.append("<td>" + displayRecords[i].toAcceptanceDate + "</td>");

            if (tableId === 'queue') {
                var actionTd = jq('<td/>');

                var btnView = jq('<button/>', {
                    text: 'Serve Client',
                    class: 'openPatient',
                    value: displayRecords[i].id
                });

                actionTd.append(btnView);

                tr.append(actionTd);
            }
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