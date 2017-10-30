package sopr.test;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.paipai.util.io.ByteStream;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.UnsupportedEncodingException;

import sopr.common.Protocol.OPRequest;
import sopr.common.Protocol.OPResponse;

/**
 * Created by zhouzhendong on 2017/5/3.
 */
public class OPJmeterSampleClient extends AbstractJavaSamplerClient {
    private SimpleTcpClient client = null;

    public static void main(String[] args) {
        OPJmeterSampleClient app = new OPJmeterSampleClient();
        JavaSamplerContext context = new JavaSamplerContext(app.getDefaultParameters());
        SampleResult sr = app.runTest(context);
        System.out.println(sr.getResponseMessage());
    }

    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("serverIp", "10.190.31.107");
        params.addArgument("serverPort", "6000");
        params.addArgument("label", "OPSample");
        params.addArgument("query", "nike");
        params.addArgument("role", "3");
        params.addArgument("channel", "255");
        return params;
    }

    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
    }

    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);
    }

    public SampleResult runTest(JavaSamplerContext sc) {
        int ANET_PACKET_FLAG = 0x416e4574;
        int MAX_PACKET_HEADER = 32;
        String query = "nike";
        try {
            query = java.net.URLDecoder.decode(sc.getParameter("query"), "GBK");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String serverIp = sc.getParameter("serverIp");
        int serverPort = Integer.valueOf(sc.getParameter("serverPort"));
        int roleId = Integer.valueOf(sc.getParameter("role"));
        int channel = Integer.valueOf(sc.getParameter("channel"));
        String label = sc.getParameter("label");

        SampleResult sr = new SampleResult();
        sr.setSampleLabel(label);
        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(serverIp);
        sb.append(" Port: ").append(serverPort);
        sb.append("\n");
        sr.setSamplerData(sb.toString());

        OPRequest.Builder reqBuilder = OPRequest.newBuilder();
        OPResponse ret = null;

        byte[] bodyBuff = new byte[1024];
        sr.sampleStart();
        try {
            if (client == null) {
                client = new SimpleTcpClient(serverIp, serverPort, 100);
            }

            reqBuilder.setKeyword(ByteString.copyFrom(query.getBytes("UTF8")));
            reqBuilder.setChannel(channel);
            reqBuilder.setRoleid(roleId);
            reqBuilder.setCharset(OPRequest.Charset.UTF8);
            reqBuilder.setClassid1(0);
            reqBuilder.setClassid2(0);
            reqBuilder.setClassid3(0);
            OPRequest req = reqBuilder.build();


            byte[] buffer = new byte[4 * 4 + MAX_PACKET_HEADER + req.getSerializedSize()];
            CodedOutputStream outputBody = CodedOutputStream.newInstance(bodyBuff, 0, 1024);
            req.writeTo(outputBody);
            ByteStream bs = new ByteStream(buffer, buffer.length);
            // write header
            bs.pushInt(ANET_PACKET_FLAG);
            bs.pushInt(1);//header->_chid
            bs.pushInt(0);//header->_pcode
            bs.pushInt(req.getSerializedSize() + MAX_PACKET_HEADER);
            for (int i = 0; i < (MAX_PACKET_HEADER / 4); i++) {
                bs.pushInt(0);
            }
            bs.pushBytes(bodyBuff, req.getSerializedSize());

            client.write(buffer);
            byte[] header = client.read(16);

            ByteStream rbs = new ByteStream(header, 16);
                /*int flag = */
            rbs.popInt();
                /*int chid = */
            rbs.popInt();
                /*int pcode = */
            rbs.popInt();
            int dataLen = rbs.popInt();
            byte[] body = client.read(dataLen);

            CodedInputStream in = CodedInputStream.newInstance(body,
                    MAX_PACKET_HEADER, dataLen - MAX_PACKET_HEADER);
            //parse package
            ret = OPResponse.parseFrom(in);

            if (0 != ret.getErrcode()) {
                throw new RuntimeException("OPService Response error code " + String.valueOf(ret.getErrcode()));
            }

            sr.sampleEnd();
            sr.setSuccessful(true);
            sr.setResponseCode("OK");
            if (ret != null) {
                //sr.setResponseMessage(ret.toString());
                sr.setResponseMessage("OK");
            } else {
                sr.setResponseMessage("null ret");
            }
        } catch (Exception e) {
            sr.setResponseCode("300");
            sr.setResponseMessage(e.toString());
            sr.setErrorCount(1);
            sr.setSuccessful(false);
            sr.sampleEnd();
            e.printStackTrace();
        } finally {
            if(client != null) {
                client.close();
                client = null;
            }
        }
        sr.setLatency(sr.getTime());
        return sr;
    }
}
