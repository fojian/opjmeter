package sopr.test;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.paipai.util.io.ByteStream;
import org.apache.jmeter.config.Arguments;
import sopr.common.Sopr;

/**
 * Created by zhouzhendong on 2017/5/3.
 */
public class OPSampleClient {

    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("label", "OPSample");
        params.addArgument("serverIp", "10.190.31.107");
        params.addArgument("serverPort", "6000");
        params.addArgument("label", "OPSample");
        params.addArgument("query", "nike");
        params.addArgument("role", "3");
        params.addArgument("channel", "255");
        return params;
    }

    public static void main(String[] args) {
        SimpleTcpClient client = null;


        int ANET_PACKET_FLAG = 0x416e4574;
        int MAX_PACKET_HEADER = 32;
        String query = "nike";

        String serverIp = "10.190.31.107";
        int serverPort = 6000;

        Sopr.OPRequest.Builder reqBuilder = Sopr.OPRequest.newBuilder();
        Sopr.OPResponse ret = null;

        byte[] bodyBuff = new byte[1024];

        try {
            if (client == null) {
                client = new SimpleTcpClient(serverIp, serverPort, 100);
            }

            reqBuilder.setKeyword(ByteString.copyFrom(query.getBytes()));
            reqBuilder.setChannel(255);
            reqBuilder.setRoleid(3);
            reqBuilder.setCharset(Sopr.OPRequest.Charset.UTF8);
            reqBuilder.setClassid1(0);
            reqBuilder.setClassid2(0);
            reqBuilder.setClassid3(0);
            Sopr.OPRequest req = reqBuilder.build();


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
            ret = Sopr.OPResponse.parseFrom(in);

            if (0 != ret.getErrcode()) {
                throw new RuntimeException("OPService Response error code " + String.valueOf(ret.getErrcode()));
            }

            System.out.printf("1=> %s\n", ret.toString());
            System.out.printf("2=> %s\n", ret.getContent());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(client != null) {
                client.close();
                client = null;
            }
        }
    }

}
