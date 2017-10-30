package com.jd.search.anet.client;

import java.io.UnsupportedEncodingException;

import jd.search.request.JdSearchRequest.QPRequest;
import jd.search.request.JdSearchRequest.CouponInfo;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.paipai.util.io.ByteStream;

public class QPJmeterSampleClient extends AbstractJavaSamplerClient {
    private SimpleTcpClient client = null;

	public Arguments getDefaultParameters()
    {
        Arguments params=new Arguments();
        params.addArgument("serverIp","172.16.152.27");
        params.addArgument("serverPort","9797");
        params.addArgument("query", "nokia");
        params.addArgument("flag", "47");
        params.addArgument("query_enc", "1");
        params.addArgument("label", "QPSample");
        return params;
    }
	
	public static void main(String[] args) {
		QPJmeterSampleClient app = new QPJmeterSampleClient();
		JavaSamplerContext context = new JavaSamplerContext(app.getDefaultParameters());
		SampleResult sr = app.runTest(context);
		System.out.println(sr.getResponseMessage());
	}
	
	public void setupTest(JavaSamplerContext context){
        super.setupTest(context);
    }
	
	public void teardownTest(JavaSamplerContext context){
        super.teardownTest(context);
    }

	public SampleResult runTest(JavaSamplerContext sc){
	    int ANET_PACKET_FLAG = 0x416e4574;
		int MAX_PACKET_HEADER = 32;
		String query="nike";
		try {
			query = java.net.URLDecoder.decode(sc.getParameter("query"), "GBK");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		boolean is_coupon = false;
		if (query.endsWith("#coupon#"))
		{
			is_coupon = true;
		}
		String label = sc.getParameter("label");
		String serverIp=sc.getParameter("serverIp");
		int serverPort=Integer.valueOf(sc.getParameter("serverPort"));
        SampleResult sr=new SampleResult();
        sr.setSampleLabel(label);
        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(serverIp);
        sb.append(" Port: ").append(serverPort);
        sb.append("\n");
        sr.setSamplerData(sb.toString());
        //sr.sampleStart();
        byte[] bodyBuff = new byte[1024];
        QPRequest.Builder reqBuilder = QPRequest.newBuilder();
        QPRequest ret = null;
    	sr.sampleStart();
        try {
        	if (client == null)
        	{
                client = new SimpleTcpClient(serverIp, serverPort, 100);
        	}
			if (is_coupon)
			{
				CouponInfo.Builder couponBuilder = CouponInfo.newBuilder();
				couponBuilder.setCouponId(Long.valueOf(query.split("#")[0]));
				couponBuilder.setCouponOpOnly(true);
				couponBuilder.setCouponType(1);
				reqBuilder.setConponQuery(couponBuilder.build());
			}

			reqBuilder.setOriginalKey(ByteString.copyFrom((query.getBytes("GBK"))));

        	QPRequest req = reqBuilder.build();
            byte[] buffer = new byte[4*4 + MAX_PACKET_HEADER + req.getSerializedSize()];
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
			{
				ByteStream rbs = new ByteStream(header, 16);
				/*int flag = */rbs.popInt();
				/*int chid = */rbs.popInt();
				/*int pcode = */rbs.popInt();
				int dataLen = rbs.popInt();
				byte[] body = client.read(dataLen);
				{
					CodedInputStream in = CodedInputStream.newInstance(body,
							MAX_PACKET_HEADER, dataLen - MAX_PACKET_HEADER);
					//parse package
					ret = QPRequest.parseFrom(in);
				}
			}
        	sr.sampleEnd();
            sr.setSuccessful(true);
            sr.setResponseCode("200");
            if (ret != null)
            {
            	sr.setResponseMessage(ret.toString());
            }
            else
            {
            	sr.setResponseMessage("null ret");
            }
		} catch (Exception e) {
        	sr.setResponseCode("300");
    		sr.setResponseMessage(e.toString()); 
    		sr.setErrorCount(1);
            sr.setSuccessful(false);
            sr.sampleEnd();
		}
        sr.setLatency(sr.getTime());
		return sr;
	}
}
