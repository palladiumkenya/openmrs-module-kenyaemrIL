<script type="text/javascript">
    jq = jQuery;

    jq(function() {
        jq("#showStatus").hide();
        jq("#refresh-msg").show();
        jq("#error-msg").hide();

        jq('#refresh').click(function() {
            jq("#msgSpan").text("Refreshing IL Messages");
            jq("#showStatus").show();
            jq("#msg").text("");
            jq("#refresh-msg").show();
            jq("#error-msg").hide();
            jq("#refresh").prop("disabled", true);
            jq("#errorQueue").prop("disabled", true);
            jq.getJSON('${ ui.actionLink("refreshTables") }')
                .success(function(data) {
                    jq("#showStatus").hide();
                    jq("#msg").text("IL messages refreshed successfully");
                    jq("#refresh").prop("disabled", false);
                    jq("#errorQueue").prop("disabled", false);
                    for (index in data) {
                        jq('#log_table > tbody > tr').remove();
                        var tbody = jq('#log_table > tbody');
                        for (index in data) {

                            var item = data[index];
                            var row = '<tr>';
                            row += '<td width="20%">' + item.date_created + '</td>';
                            row += '<td width="20%">' + item.message_type + '</td>';
                            row += '<td width="20%">' + item.source + '</td>';
                            row += '<td width="20%">' + item.status + '</td>';
                           // row += '<td width="20%">' + item.error + '</td>';
                            row += '</tr>';
                            tbody.append(row);
                        }
                    }
                })
                .error(function(xhr, status, err) {
                    jq("#showStatus").hide();
                    jq("#msg").text("There was an error refreshing IL messages");
                    jq("#refresh").prop("disabled", false);
                    jq("#errorQueue").prop("disabled", false);
                    alert('AJAX error ' + err);
                })
        });
        jq('#errorQueue').click(function() {
            //jq("#error-msg").empty();
            jq("#errorQueue").attr("disabled", false);
            jq("#msgSpan").text("Refreshing error Messages");
            jq("#showStatus").show();
            jq("#refresh-msg").hide();
            jq("#error-msg").show();
            jq("#msg").text("");
            jq("#refresh").prop("disabled", true);
            jq("#errorQueue").prop("disabled", true);
            jq.getJSON('${ ui.actionLink("errorMessages") }')
                .success(function(data) {
                    console.log("Print data ==>"+data);
                    if (data !="") {
                        console.log("Data is populated ==>");
                    jq("#showStatus").hide();
                    jq("#msg").text("IL error messages refreshed successfully");
                    jq("#refresh").prop("disabled", false);
                    jq("#errorQueue").prop("disabled", false);
                    for (index in data) {
                        jq('#error_log_table > tbody > tr').remove();
                        var tbody = jq('#error_log_table > tbody');
                        for (index in data) {

                            var item = data[index];
                            var row = '<tr>';
                            row += '<td width="20%">' + item.date_created + '</td>';
                            row += '<td width="20%">' + item.message_type + '</td>';
                            row += '<td width="20%">' + item.source + '</td>';
                            row += '<td width="20%">' + item.status + '</td>';
                            row += '<td width="20%">' + item.error + '</td>';
                            row += '</tr>';
                            tbody.append(row);
                        }
                    }
                }else{
                        jq("#error-msg").empty();
                        jq("#refresh").prop("disabled", false);
                        jq("#errorQueue").prop("disabled", false);
                        jq("#showStatus").hide();
                        jq("#msg").text("IL error messages refreshed successfully");
                        console.log("Data is empty ==>");
                    }
                })
                .error(function(xhr, status, err) {
                    jq("#showStatus").hide();
                    jq("#msg").text("There was an error refreshing IL messages");
                    jq("#refresh").prop("disabled", false);
                    jq("#errorQueue").prop("disabled", false);
                    alert('AJAX error ' + err);
                })
        });
    });
</script>
<style>
table {
    width: 100%;
}

th, td {
    padding: 5px;
    text-align: left;
    height: 30px;
    border-bottom: 1px solid #ddd;
}

tr:nth-child(even) {background-color: #f2f2f2;}
</style>
<hr>
<div>
<table>
    <tr>
        <td>
    <button id="refresh">
        <img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" /> Recent messages
    </button>
        </td>
        <td>
     <button id="errorQueue">
        <img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" /> Error messages
     </button>
        </td>
    </tr>
</table>
</div>
<br/>
<div id="showStatus">
    <span id="msgSpan"></span> &nbsp;&nbsp;<img src="${ ui.resourceLink("kenyaui", "images/loader_small.gif") }"/>
</div>
<div id="msg"></div>
<div id="refresh-msg">
    <h3>History of IL Messages (Last 10 entries)</h3>
    <table id="log_table">
        <thead>
        <tr>
            <th>Date</th>
            <th>Message Type</th>
            <th>Source</th>
            <th>Status</th>
        </tr>
        </thead>
        <tbody class='scrollable'>
        <% if (logs) { %>
        <% logs.each { log -> %>
        <tr>
            <td>${ log.date_created }</td>
            <td>${ log.message_type }</td>
            <td>${ log.source }</td>
            <td>${ log.status }</td>
        </tr>
        <% } %>
        <% } else { %>
        <tr>
            <td colspan="4">No record found. Please refresh for details</td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>

<div id="error-msg">
    <h3>History of IL Messages (Last 10 entries)</h3>
    <table id="error_log_table">
        <thead>
        <tr>
            <th>Date</th>
            <th>Message Type</th>
            <th>Source</th>
            <th>Status</th>
            <th>Error Status</th>
        </tr>
        </thead>
        <tbody class='scrollable'>
        <% if (logs) { %>
        <% logs.each { log -> %>
        <tr>
            <td>${ log.date_created }</td>
            <td>${ log.message_type }</td>
            <td>${ log.source }</td>
            <td>${ log.status }</td>
            <td>${ log.error }</td>
        </tr>
        <% } %>
        <% } else { %>
        <tr>
            <td colspan="4">No record found. Please refresh for details</td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>

