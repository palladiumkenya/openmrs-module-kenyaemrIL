    <%

    %>

    <style type="text/css">


    table.moh257 {
        border-collapse: collapse;
        background-color: #D9F4D3;
        width: 75%;
    }
    table.moh257 > tbody > tr > td, table.moh257 > tbody > tr > th {
        border: 1px solid black;
        vertical-align: baseline;
        padding: 2px;
        text-align: left;
        background-color: #D9F4D3;
    }

    </style>

    <div class="ke-panel-frame">
        <div class="ke-panel-heading">Patient SHR Summary</div>
        <div class="ke-panel-content" style="background-color: #D9F4D3">
            <table id="tblDetails" class="moh257" align="center" border="1" cellpadding="0" cellspacing="0">
                <tr>
                    <td colspan="3">&nbsp;&nbsp;</td>
                </tr>

                   <tr>
                     <td>Weight (Kgs): 67</td>
                      <td colspan="2">TB Screening Outcome: Negative</td>
                   </tr>
                 <tr>
                    <td>Height (cm): 178</td>
                    <td colspan="2">Chronic Illness:: NCD</td>
                </tr>
                 <tr>
                    <td>BMI: 25.4</td>
                    <td colspan="2">OI History: </td>
                </tr>
                 <tr>
                    <td>Blood Pressure: 120/80</td>
                    <td colspan="2">STI Screening: </td>
                </tr>

                 <tr>
                   <td>Oxygen Saturation: 98%</td>
                    <td id = "cacx-struct" colspan="2">CACX Screening: Negative</td>

                </tr>

                <tr>
                   <td>Respiratory rate: 21/02/2020</td>
                   <td colspan="2">TPT Start Date: 21/02/2020</td>
                </tr>
                <tr>
                   <td>Pulse Rate: 21/02/2020</td>
                   <td colspan="2">TPT Completion Date: 21/02/2020</td>
                </tr>

                <tr>
                     <td colspan="3">&nbsp;</td>
                </tr>

                <tr>
                    <td colspan="3">Drug allergies: Dust,pollen</td>
                </tr>
                <tr>
                    <td colspan="3">&nbsp;</td>
                </tr>
                <tr>
                    <td colspan="3">&nbsp;</td>
                </tr>

                <tr>
                    <td>Current ART regimen: </td>
                    <td colspan="2">
                        <table width="100%">
                            <tr>
                                <td width="40%">ART interruptions:</td>
                                <td>
                                    <table>
                                        <tr>
                                            <td>Reason: </td>
                                        </tr>
                                        <tr>
                                            <td>Date:</td>
                                        </tr>
                                    </table>
                                </td>

                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td>Current WHO stage: </td>
                    <td colspan="2">
                        <table width="100%">
                            <tr>
                                <td width="40%">Substitution within 1st line regimen:</td>
                                <td>
                                    <table>
                                        <tr>
                                            <td>Reason: </td>
                                        </tr>
                                        <tr>
                                            <td>Date:</td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>

                    </td>
                </tr>
                <tr>
                    <td>CTX: </td>
                    <td colspan="2">
                        <table width="100%">
                            <tr>
                                <td width="40%">Switch to 2nd line regimen:</td>
                                <td>
                                    <table>
                                        <tr>
                                            <td>Reason: </td>
                                        </tr>
                                        <tr>
                                            <td>Date:</td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>

                <tr>
                    <td colspan="2">
                        <table width="75%">
                            <tr>
                                <td width="50%">Most recent CD4:</td>
                                <td>Date: </td>
                            </tr>
                        </table>


                    </td>
                    <td>
                        Death date:
                    </td>
                </tr>
                    <tr>



                </tr>
                <tr>
                    <td colspan="2">
                        <table width="75%">
                            <tr>
                                <td width="50%">Most recent VL: </td>
                                <td> Date: </td>
                            </tr>
                        </table>
                    </td>
                    <td>
                        Next appointment:
                    </td>
                </tr>
                <tr>
                  <td colspan="3">&nbsp;</td>
                </tr>
                                <tr>
                                   <td colspan="2">Viral Load Trends</td>
                                    <td colspan="2">CD4 Trends</td>
                                </tr>
                                <tr>

                                </tr>
                                <tr>
                                      <td colspan="2">
                                         <table width="75%">
                                            <tr>
                                                 <td> VL Dates</td>
                                                 <td> Result</td>
                                            </tr>
                                            <tr>

                                            </tr>
                                        </table>
                                      </td>

                                 <td colspan="2">
                                    <table width="75%">
                                       <tr>
                                         <td> CD4 Dates</td>
                                         <td> Result</td>
                                       </tr>
                                        <tr>

                                       </tr>
                                    </table>
                                </td>
                                </tr>
                <tr>
                    <td>Clinical Notes: </td>
                    <td colspan="2">

                    </td>
                </tr>
            </table>
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
