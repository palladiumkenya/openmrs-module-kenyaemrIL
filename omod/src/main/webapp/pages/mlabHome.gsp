<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])
    def menuItems = [
            [label: "Back", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyaemr", "userHome")],
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
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Back", items: menuItems])}
</div>

<div class="ke-page-content">

    <div>
        <fieldset>
            <legend>MLAB summary</legend>
            <div>
                <table class="simple-table" width="100%">
                    <thead>
                    </thead>
                    <tbody>
                    <tr>
                        <td width="15%">Total data in queue</td>
                        <td>${queueListSize}</td>
                    </tr>
                    <tr>
                        <td width="15%">Total errors</td>
                        <td>${generalErrorListSize}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </fieldset>
    </div>

    <div id="program-tabs" class="ke-tabs">

        <div class="ke-tabmenu">

            <div class="ke-tabmenu-item" data-tabid="queue_data">Viral Load results</div>

            <div class="ke-tabmenu-item" data-tabid="general_error_queue">Viral Load error queue</div>

        </div>

        <div class="ke-tab" data-tabid="queue_data">
            <table cellspacing="0" cellpadding="0" width="100%">
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
                                            <th class="cccNumberColumn">Date sample collected</th>
                                            <th class="sampleTypeColumn">Sample type</th>
                                            <th class="dateRequestColumn">Date tested</th>
                                            <th class="dateRequestColumn">Result</th>
                                            <th class="dateRequestColumn">Date created</th>
                                            <th class="actionColumn"></th>
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
        
        <div class="ke-tab" data-tabid="general_error_queue">
            <table id="general-error-queue-data" cellspacing="0" cellpadding="0" width="100%">
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
                                                <th class="cccNumberColumn">Date sample collected</th>
                                                <th class="sampleTypeColumn">Sample type</th>
                                                <th class="dateRequestColumn">Date tested</th>
                                                <th class="dateRequestColumn">Result</th>
                                                <th class="dateRequestColumn">Error</th>
                                                <th class="actionColumn">
                                                    <input type="button" id="requeueGeneralErrors" value="Re-queue" disabled/>
                                                    <input type="button" id="deleteGeneralErrors" value="Delete" disabled/>
                                                </th>
                                            </tr>

                                            </thead>
                                            <tbody id="general-error-list">

                                            </tbody>

                                        </table>

                                        <div id="pager">
                                            <ul id="generalErrorPagination" class="pagination-sm"></ul>
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

    var selectedGeneralErrors = [];
    var selectedRegistrationErrors = [];
    //On ready
    jq = jQuery;
    jq(function () {
        // apply pagination

        var generalErrorPaginationDiv = jq('#generalErrorPagination');
        var queuePaginationDiv = jq('#queuePagination');

        var generalErrorListDisplayArea = jq('#general-error-list');
        var queueListDisplayArea = jq('#queue-list');

        var numberOfGeneralErrorRecords = ${ generalErrorListSize };
        var numberOfRecordsToProcess = ${ queueListSize };

        var generalErrorRecords = ${ generalErrorList };
        var queueRecords = ${ queueList };

        var generalErrorDataDisplayRecords = [];
        var queueDataDisplayRecords = [];

        var recPerPage = 10;
        var generalErrorStartPage = 1;

        var queueStartPage = 1;
        var totalGeneralErrorPages = Math.ceil(numberOfGeneralErrorRecords / recPerPage);
        var totalQueuePages = Math.ceil(numberOfRecordsToProcess / recPerPage);

        var visibleGeneralErrorPages = 1;
        var visibleQueuePages = 1;

        var sendCount = 0;

        if (totalGeneralErrorPages <= 5) {
            visibleGeneralErrorPages = totalGeneralErrorPages;
        } else {
            visibleGeneralErrorPages = 5;
        }


        if (totalQueuePages <= 5) {
            visibleQueuePages = totalQueuePages;
        } else {
            visibleQueuePages = 5;
        }


        if(numberOfRecordsToProcess > 0) {
            apply_pagination(queuePaginationDiv, queueListDisplayArea, totalQueuePages, visibleQueuePages, queueRecords, queueDataDisplayRecords, 'queue', queueStartPage); // records in queue
        }

        if (numberOfGeneralErrorRecords > 0) {
            apply_pagination(generalErrorPaginationDiv, generalErrorListDisplayArea, totalGeneralErrorPages, visibleGeneralErrorPages, generalErrorRecords, generalErrorDataDisplayRecords, 'general-error', generalErrorStartPage); // general records in error
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

        //Enable or Disable Requeue and Delete button on incoming queue depending on condition of queue item selection 
        jq(".selectGeneralElement").change(function() {
            let len = jq('.selectGeneralElement:checked').length;
            if (len > 0) {
                jq('#requeueGeneralErrors').attr('disabled', false);
                jq('#deleteGeneralErrors').attr('disabled', false);
            } else {
                jq('#requeueGeneralErrors').attr('disabled', true);
                jq('#deleteGeneralErrors').attr('disabled', true);
            }
        });

        //Enable or Disable Requeue and Delete button on incoming queue depending on condition of queue item selection 
        jq("#chk-general-select-all").change(function() {
            let len = jq('.selectGeneralElement:checked').length;
            if (len > 0) {
                jq('#requeueGeneralErrors').attr('disabled', false);
                jq('#deleteGeneralErrors').attr('disabled', false);
            } else {
                jq('#requeueGeneralErrors').attr('disabled', true);
                jq('#deleteGeneralErrors').attr('disabled', true);
            }
        });

        // population general error selection list
        jq(document).on('click','.selectGeneralElement',function () {
            var queueUuid = jq(this).val();
            if (jq(this).is(":checked")) {
                selectedGeneralErrors.push(queueUuid);
            }
            else {
                 var elemIndex = selectedGeneralErrors.indexOf(queueUuid);
                 if (elemIndex > -1) {
                    selectedGeneralErrors.splice(elemIndex, 1);
                 }
                 jq('#chk-general-select-all').prop('checked', false);
             }
        });

        // handle general select all
        jq(document).on('click','#chk-general-select-all',function () {
            //clear selection list
            selectedGeneralErrors = [];
            if(jq(this).is(':checked')) {
                jq('.selectGeneralElement').prop('checked', true);
                // populate the list with all elements
                for (var i = 0; i < generalErrorRecords.length; i++) {
                    let uuid = generalErrorRecords[i].uuid;
                    selectedGeneralErrors.push(uuid);
                }
            }
            else {
                jq('.selectGeneralElement').prop('checked', false);
            }
        });

        // handles button that re-queues general errors
        jq(document).on('click','#requeueGeneralErrors',function () {
            AsyncConfirmYesNo("Please Confirm", "Are you sure you want to requeue?", requeueGeneralErrors, () => {});        
        });

        // re-queues general errors once user confirms
        function requeueGeneralErrors() {
            if(selectedGeneralErrors.length > 0) {
                jq('#showWaitBox').modal('show');
                requeueAll(selectedGeneralErrors);         
            }
            jq('#chk-general-select-all').prop('checked', false);
        }

        // handles button for deleting general errors
        jq(document).on('click','#deleteGeneralErrors',function () {
            AsyncConfirmYesNo("Please Confirm", "Are you sure you want to delete?", deleteGeneralErrors, () => {});
        });

        // deletes general errors once user confirms
        function deleteGeneralErrors() {
            if(selectedGeneralErrors.length > 0) {
                jq('#showWaitBox').modal('show');
                deleteAll(selectedGeneralErrors);
            }
            jq('#chk-general-select-all').prop('checked', false);
        }


        // deletes all the given error items
        function deleteAll(selectedErrors) {
            //Delete the selected errors, 10 at a time
            let count = 10;
            sendCount = 0;
            let totalItems = selectedErrors.length;
            let loopThrough = (totalItems > 0) ? totalItems : 1;
            let pages = Math.ceil(loopThrough * 1.00 / count * 1.00);

            for (var i = 0; i < pages; i++) {
                let begin = i * count;
                let end = begin + count;
                let sliced = selectedErrors.slice(begin, end);
                let listToSubmit = sliced.join();
                // lets delete this page
                ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'purgeErrors', { errorList : listToSubmit }, function (result) {
                    sendCount++;
                    console.log("Delete items: Finished sending page. Sendcount: " + sendCount);
                    if(sendCount >= pages)
                    {
                        jq('#showWaitBox').modal('hide').promise().done( function () {
                            AsyncShowInfo("Success", "Successfully Deleted", reloadPage); 
                        } )
                    }
                });
            }
        }

        // requeues all the given error items
        function requeueAll(selectedErrors) {
            //Requeue the selected errors, 10 at a time
            let count = 10;
            sendCount = 0;
            let totalItems = selectedErrors.length;
            let loopThrough = (totalItems > 0) ? totalItems : 1;
            let pages = Math.ceil(loopThrough * 1.00 / count * 1.00);

            for (var i = 0; i < pages; i++) {
                let begin = i * count;
                let end = begin + count;
                let sliced = selectedErrors.slice(begin, end);
                let listToSubmit = sliced.join();
                // lets requeue this page
                ui.getFragmentActionAsJson('afyastat', 'mergePatients', 'requeueErrors', { errorList : listToSubmit }, function (result) {
                    sendCount++;
                    if(sendCount >= pages)
                    {
                        jq('#showWaitBox').modal('hide').promise().done( function () {
                            AsyncShowInfo("Success", "Successfully Requeued", reloadPage); 
                        } )
                    }
                });
            }
        }

    });

    function generate_table(displayRecords, displayObject, tableId) {
        var tr;
        displayObject.html('');
        for (var i = 0; i < displayRecords.length; i++) {

            tr = jq('<tr/>');
            tr.append("<td>" + displayRecords[i].cccNumber + "</td>");

            tr.append("<td>" + displayRecords[i].sampleCollectionDate + "</td>");
            tr.append("<td>" + displayRecords[i].sampleType + "</td>");
            tr.append("<td>" + displayRecords[i].testDate + "</td>");
            tr.append("<td>" + displayRecords[i].result + "</td>")
            if (tableId !== 'general-error') {
                tr.append("<td>" + displayRecords[i].dateCreated + "</td>");
            }
            if (tableId === 'general-error') {
                tr.append("<td>" + displayRecords[i].error + "</td>");
            }

            if (tableId === 'general-error') {
                var selectTd = jq('<td/>');
                var selectCheckbox = jq('<input/>', {
                    type: 'checkbox',
                    class: 'selectGeneralElement',
                    value: displayRecords[i].uuid
                });

                selectTd.append(selectCheckbox);
                tr.append(selectTd);
            }

            displayObject.append(tr);
        }
    }


</script>