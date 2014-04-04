import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.*;

class SnmpRequest {
    // device to to be accessed
    Device dev = null;

    TransportMapping        transport    = null;
    CommunityTarget            comTarget    = null;
    Snmp                    snmp        = null;
    static Integer32        snmpRequestID    = new Integer32(0);
    
    public SnmpRequest(Device dev) throws IOException {
        this.dev = dev;

        // create transport mapping
        transport = new DefaultUdpTransportMapping();
        transport.listen();

        // create target address object
        comTarget = new CommunityTarget();
        comTarget.setCommunity(new OctetString(community));
        comTarget.setVersion(dev.snmpVersion);
        comTarget.setAddress(new UdpAddress(dev.ipAddress + "/" + dev.port));
        comTarget.setRetries(0);
        comTarget.setTimeout(3000);

        snmp = new Snmp(transport);
    }
    
    /**
     * send snmp get request, including only one VariableBinding
     * @oidValue oidValue specifies the managed obj
     * @String return value of managed obj, if return value is
     *   'noSuchObject', it means the managed obj is not processed by the snmp
     *   agent, if it is null, it means snmp agent didn't response.
     */
    public String sendGetRequest(String oidValue) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        
        pdu.setType(PDU.GET);
        pdu.setRequestID(snmpRequestID);
        snmpRequestID.setValue(snmpRequestID.toInt()+1);
        
        ResponseEvent response = snmp.get(pdu, comTarget);
        
        String responseValue = null;
        if (response != null) {
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                int status = responsePDU.getErrorStatus();
                int index = responsePDU.getErrorIndex();
                String text = responsePDU.getErrorStatusText();

                if (status == PDU.noError) {
                    responseValue = responsePDU.
                            getVariableBindings().elementAt(0).getVariable().toString();
                }
                else {
                    //System.out.println("Error: GET Request failed");
                    //System.out.println("Error Status = " + status);
                    //System.out.println("Error Index = " + index);
                    //System.out.println("Error Status Text = " + text);
                }
            }
            else {
                //System.out.println("Error: Response PDU is null");
            }
        }
        else {
            //System.out.println("Error: Agent timeout");
        }

        return responseValue;    
    }
    
    /**
     * send snmp get request, including more than one VariableBinding
     * @oids[] specifies the managed objs
     * @Vector<VariableBindding> return null if snmp agent doesn't response,
     *  or return the response value, in which every VariableBinding contains
     *  the value associated with one oid. 
     */
    public Vector<VariableBinding> sendGetRequest(String oids[]) throws IOException {

        Vector<VariableBinding> vector = null;
        
        int length = oids.length;
        if (length > 0) {
            PDU pdu = new PDU();

            for (int i = 0; i < length; i++) {
                pdu.add(new VariableBinding(new OID(oids[i])));
            }

            pdu.setType(PDU.GET);
            pdu.setRequestID(snmpRequestID);
            snmpRequestID.setValue(snmpRequestID.toInt()+1);

            ResponseEvent response = snmp.get(pdu, comTarget);

            if (response != null) {
                PDU responsePDU = response.getResponse();

                if (responsePDU != null) {
                    int status = responsePDU.getErrorStatus();
                    int index = responsePDU.getErrorIndex();
                    String text = responsePDU.getErrorStatusText();

                    if (status == PDU.noError)
                        vector = (Vector<VariableBinding>) responsePDU.getVariableBindings();
                    else {
                        //System.out.println("Error: Request Failed");
                        //System.out.println("Error Status = "+status);
                        //System.out.println("Error Index = "+index);
                        //System.out.println("Error Status Text = "+text);
                    }
                }
                else {
                    //System.out.println("Error: Response PDU is null");
                }
            }
            else {
                //System.out.println("Error: Agent Timeout... ");
            }
        }

        return vector;
    }
    
    /**
     * send snmp get next request, including only one VariableBinding
     * @oidValue oidValue specifies the managed obj
     * @String return null if snmp agent doesn't response, or return the
     *  response value.
     */
    public String sendGetNextRequest(String oidValue) throws IOException {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        
        pdu.setType(PDU.GET);
        pdu.setRequestID(snmpRequestID);
        snmpRequestID.setValue(snmpRequestID.toInt()+1);

        ResponseEvent response = snmp.getNext(pdu, comTarget);
        
        String responseValue = null;
        if (response != null) {
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                int status = responsePDU.getErrorStatus();
                int index = responsePDU.getErrorIndex();
                String text = responsePDU.getErrorStatusText();

                if (status == PDU.noError) {
                    //responseValue = responsePDU.getVariableBindings().toString();
                    responseValue = 
                    ((VariableBinding)responsePDU.getVariableBindings().elementAt(0))
                    .getVariable().toString();
                }
                else {
                    //System.out.println("Error: Request Failed");
                    //System.out.println("Error Status = " + status);
                    //System.out.println("Error Index = " + index);
                    //System.out.println("Error Status Text = " + text);
                }
            }
            else {
                //System.out.println("Error: GetNextResponse PDU is null");
            }
        }
        else {
            //System.out.println("Error: Agent Timeout");
        }

        return responseValue;
    }    
    
    /**
     * send snmp get bulk request, including only one VariableBinding
     * @oidValue oidValue Specifies the managed obj
     * @LinkedHashMap<String, String> return null if snmp agent doesn't
     *  response, or return the value, in which every Map<K,V> is a <oid,value>
     *  pair.
     */
    public LinkedHashMap<String, String> sendGetBulkRequest(String oidValue) {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidValue)));
        pdu.setType(PDU.GETBULK);
        pdu.setRequestID(snmpRequestID);
        snmpRequestID.setValue(snmpRequestID.toInt()+1);
        pdu.setRequestID(snmpRequestID);
        
        /*
        SNMPGETBULK, with MAXREPETITONS and NONREPEATERS in GETBULK 
        PDU, there's a var list for NONREPEATERS vars, only one GETNEXT request 
        is done for rest vars, for each of them, process GETNEXT request 
        MAXREPTITIONS times.

        in our getbulk request, only one oid is specified, so NONREPEATER is set to 
        ZERO we want to get data as much as possible, so set MAXREPETITONS to 
        1000.
        
        this parameter should be adjustable.
        */
        pdu.setMaxRepetitions(500);
        pdu.setNonRepeaters(0);

        /*
        using HashMap, the sequence you put and you traverse may be different, 
        using LinkedHashMap instead of HashMap.
        */
        LinkedHashMap<String, String> map = null;

        // Snmp call getBulk
        ResponseEvent response = null;
        try {
            response = snmp.getBulk(pdu, comTarget);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        if (response != null) {
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                map = new LinkedHashMap<String, String>();
            
                int status = responsePDU.getErrorStatus();
                int index = responsePDU.getErrorIndex();
                String text = responsePDU.getErrorStatusText();

                if (status == PDU.noError) {
                    Vector vc = responsePDU.getVariableBindings();
                    for (int i = 0; i < vc.size(); i++) {
                        VariableBinding vb = (VariableBinding)vc.elementAt(i);
                        // "." is removed from the beginning, pay attention
                        String oid = vb.getOid().toString();
                        String value = vb.getVariable().toString();

                        map.put(oid, value);
                    }
                }
                else {
                    //System.out.println("Error: Request Failed");
                    //System.out.println("Error Status = " + status);
                    //System.out.println("Error Index = " + index);
                    //System.out.println("Error Status Text = " + text);
                }
            }
            else {
                //System.out.println("OID: "+oidValue+" Error: Response PDU is null");
            }
        }
        else {
            //System.out.println("Error: Agent Timeout");
        }
        return map;
    }    
    
    /**
     * send snmp get table request, which can get only one table
     * @oidValue oidValue specifies the managed obj
     * @ArrayList<LinkedHashMap<String, String>> return null if snmp agent
     * doesn't response, or return response table value, in which every
     * element is associated with one row in the table.
     */
    public ArrayList<LinkedHashMap<String, String>>
    sendGetTableRequest(String oidValue) {
        
        ArrayList<LinkedHashMap<String,String>> table = null;
        LinkedHashMap<String,String> newTableDataMap = null;
        
        boolean hasNext = true;
        
        String curOidValue = oidValue;
        String nextOidValue = oidValue;
        
        while(hasNext) {
            
            curOidValue = nextOidValue;
            newTableDataMap = sendGetBulkRequest(curOidValue);
            
            // 第一次getbulk
            if(table==null) {
                if(newTableDataMap==null)        // 未收到响应
                    return null;
                else                                            // 收到了响应
                    table = new ArrayList<LinkedHashMap<String, String>>();
            }
            
            // getbulk请求收到了响应，将结果写入table
            if(table!=null && newTableDataMap!=null) {
                /*
                     put table elements in newTableDataMap into table, remained 
                     elements that not belong table will be dropped.
                 */
                for(Map.Entry<String, String> entry : newTableDataMap.entrySet()) {
                    
                    String oid = entry.getKey();
                    String value = entry.getValue();
                    
                    // check whether current oid is within the subtree of oidValue.
                    
                    if(("."+oid).startsWith(oidValue)                     
                            && ("."+oid).contains(oidValue+".")) {
                        
                        // only need the leaf nodes, i.e, the table data
                        if(!("."+oid).endsWith(".0")) {                        
                            // ready for next GETBULK loop
                            nextOidValue = "."+oid;
                            
                            int rowNumber = Integer.parseInt(
                                    oid.substring(oid.lastIndexOf(".")+1));
                            if (rowNumber > table.size()) {
                                // new row
                                LinkedHashMap<String, String> newRow = 
                                        new LinkedHashMap<String, String>();
    
                                int tableRowsQuantity = table.size();
                                /*
                                    index may not end with order, such as 1,3,6,
                                    9 rather than 1,2,3,4, so fill the table
                                    with useless rows.
                                */
                                for (int j = 0; rowNumber-tableRowsQuantity-j>1; j++)
                                    table.add(null);
    
                                newRow.put(oid, value);
                                table.add(newRow);
                            }
                            else {
                                // existent row
                                // existent row may be 'null'
                                if(table.get(rowNumber-1)==null)
                                    table.set( rowNumber-1, 
                                            new LinkedHashMap<String, String>());
                                
                                table.get(rowNumber-1).put(oid, value);
                            }
                        }    
                        
                    }
                    else {
                         // current oid is exceed the range of table data, so 
                        // dropped it and exit the getBulk loop.
                        hasNext = false;
                        break;
                    }
                }
            }
            else {
                // here shouldn't be reached
                hasNext = false;
            }
            
            // ready for next GETBULK loop
            newTableDataMap = null;
        }
        
        return table;
    }
    
    /**
     * send snmp get table request, which can get several tables
     * @oids[] oids specifies the managed objs
     * @Vector<ArrayList<LinkedHashMap<String, String>>> return null if snmp
     * agent doesn't response, or return the response table value, in which
     * every element is associated with one table.
     */
    public Vector<ArrayList<LinkedHashMap<String, String>>> 
    sendGetTableRequest(String oids[]) {

        Vector<ArrayList<LinkedHashMap<String, String>>> allTables = 
                new Vector<ArrayList<LinkedHashMap<String,String>>>();
        
        for(int i=0; i<oids.length; i++) {
            ArrayList<LinkedHashMap<String,String>> table = 
                    new ArrayList<LinkedHashMap<String,String>>();
            
            String oidValue = oids[i];
            table = sendGetTableRequest(oidValue);
            
            allTables.add(table);
        }
        
        // check whether all sendTableRequest's responses are null
        int pos = 0;
        for(; pos<allTables.size(); pos++) {
            if(allTables.get(pos)!=null)
                break;
        }
        if(pos==allTables.size())
            return null;
        
        return allTables;
    }

    /**
     * display the value of VariableBinding
     * @vb VariableBinding variable to be displayed
     */
    public void displayVariableBinding(VariableBinding vb) {
        String oid = vb.getOid().toString();
        String value = vb.getVariable().toString();
        // if value is noSuchObject, means this oid is not implemented by agent
        System.out.println(String.format("%-30s --> %30s",oid,value));
    }
    
    /**
     * display value of one table
     * @table table to be displayed
     */
    public void displayTable(ArrayList<LinkedHashMap<String, String>> table) {
        // display all table rows
        for (LinkedHashMap<String, String> row : table) {
            if (row != null) {
                for (Map.Entry<String, String> col : row.entrySet()) {
                    System.out.println(String.format("%-30s --> %30s",
                            col.getKey(),col.getValue()));
                }
            }
        }
    }
    
    /**
     * display value
     * @vc Vector var to be displayed
     * @type type==1, display response of snmpGET, type==1, display response
     *  of snmpGETTABLE.
     */
    public void displayVector (Vector vc, int type) {
         // type = 1, process response of sendGetRequest
         // type = 2, process response of sendGetTableRequest
        if(type!=1 && type!=2) {
            System.out.println("error type");
            return;
        }
        
        if(vc==null)
            return;
            
        for(int i=0; i<vc.size(); i++) {
            if(type==1) {
                System.out.println("response from snmpget "+i+":");
                displayVariableBinding((VariableBinding)vc.elementAt(i));
            }
            if(type==2) {
                System.out.println("response from snmpgettable "+i+":");
                displayTable(
                        (ArrayList<LinkedHashMap<String,String>>)vc.elementAt(i));
            }
        }
    }
    
    public void close() throws IOException {
        snmp.close();
        transport.close();
    }
}

class Device {
    // ip address
    String ip = null;
    // snmp relevant info
    int snmpVersion = -1;
    int snmpPort = -1;
    String snmpCommunity = null;
    
    public Device(
            String ip, 
            int snmpVersion,
            int snmpPort,
            String snmpCommunity) {
        this.ip = ip;
        this.snmpVersion = snmpVersion;
        this.snmpPort = snmpPort;
        this.snmpCommunity = snmpCommunity;
        
    }
}

