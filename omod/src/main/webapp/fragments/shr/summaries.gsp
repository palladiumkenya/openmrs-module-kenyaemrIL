    <%

    %>

    <style type="text/css">


    table.moh257 {
        border-collapse: collapse;
        width: 75%;
    }
    table.moh257 > tbody > tr > td, table.moh257 > tbody > tr > th {
        border: 1px solid black;
        vertical-align: baseline;
        padding: 2px;
        text-align: left;
    }

    </style>

    <div class="ke-panel-frame">
        <div class="ke-panel-heading">Patient SHR Summary</div>
        <div class="ke-panel-content">
            <div>
                <fieldset>
                    <legend>Vitals</legend>
                    <table>
                        <% if (vitalsObs) { %>
                        <% vitalsObs.each { it -> %>
                        <tr>
                            <td>${it.display}</td>
                            <td>${it.value}</td>
                            <td>${it.date}</td>
                        </tr>
                        <% } %>

                        <% } %>
                    </table>
                </fieldset>
                <fieldset>
                    <legend>Complaints</legend>
                    <table>
                        <% if (complaints) { %>
                        <% complaints.each { it -> %>
                        <tr>
                            <td>${it.display}</td>
                            <td>${it.value}</td>
                            <td>${it.date}</td>
                        </tr>
                        <% } %>

                        <% } %>
                    </table>
                </fieldset>
                <fieldset>
                    <legend>Diagnosis</legend>
                    <table>
                        <% if (diagnosis) { %>
                        <% diagnosis.each { it -> %>
                        <tr>
                            <td>${it.display}</td>
                            <td>${it.value}</td>
                            <td>${it.date}</td>
                        </tr>
                        <% } %>

                        <% } %>
                    </table>
                </fieldset>
                <fieldset>
                    <legend>Lab Investigations</legend>
                    <table>
                        <% if (labObs) { %>
                        <% labObs.each { it -> %>
                        <tr>
                            <td>${it.display}</td>
                            <td>${it.value}</td>
                            <td>${it.date}</td>
                        </tr>
                        <% } %>

                        <% } %>
                    </table>
                </fieldset>
                <fieldset>
                    <legend>Drugs</legend>
                    <table>

                    </table>
                </fieldset>
                <fieldset>
                    <legend>Appointments</legend>
                    <table>
                        <% if (appointments) { %>
                        <% appointments.each { it -> %>
                        <tr>
                            <td>${it.appointmentType}</td>
                            <td>${it.appointmentDate}</td>
                        </tr>
                        <% } %>

                        <% } %>
                    </table>
                </fieldset>
                <fieldset>
                    <legend>Allergies</legend>
                    <table>

                    </table>
                </fieldset>
                <fieldset>
                    <legend>Referrals</legend>
                    <table>

                    </table>
                </fieldset>
                <fieldset>
                    <legend>Notes</legend>
                    <table>

                    </table>
                </fieldset>
            </div>

        </div>

    </div>

    <script type="text/javascript">
        jQuery(function(){
            jQuery('#print').click(function(){
                var disp_setting="toolbar=yes,location=yes,directories=yes,menubar=yes,";
                disp_setting+="scrollbars=yes,width=1000, height=780, left=100, top=25";
                var docprint = window.open("about:blank", "_blank", disp_setting);
                var oTable = document.getElementById("tblDetails");

                docprint.document.open();
                docprint.document.write('<html><head>');
                docprint.document.write('</head><body><center>');
                docprint.document.write(oTable.parentNode.innerHTML);
                docprint.document.write('</center></body></html>');
                docprint.document.close();
                docprint.print();
                docprint.close();
            });
        });
    </script>
