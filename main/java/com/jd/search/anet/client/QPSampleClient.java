package com.jd.search.anet.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import jd.search.request.JdSearchRequest.QPRequest;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.paipai.util.io.ByteStream;

/**
 * Hello world!
 *
 */
public class QPSampleClient
{
	private SocketChannel socketChannel = null;
	private String serverIp=null;
	private int serverPort=9797;
	String query = null;
	String label = null;
	
	public Map<String, String> getDefaultParameters()
    {
		Map<String, String> params=new HashMap<String, String>();
        params.put("serverIp","172.16.152.27");
        params.put("serverPort","9797");
        params.put("query", "nokia");
        params.put("flag", "47");
        params.put("query_enc", "1");
        params.put("label", "QPSample");
        return params;
    }
	
	public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("h", "host", true, "host");
//        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("p", "port", true, "port");
//        output.setRequired(true);
        options.addOption(output);

        Option keyword = new Option("q", "query", true, "query");
//        keyword.setRequired(true);
        options.addOption(keyword);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("qp anet test java client", options);

            System.exit(1);
            return;
        }

        String host = cmd.getOptionValue("h");
        String port = cmd.getOptionValue("p");
        String query = cmd.getOptionValue("q");
		QPSampleClient app = new QPSampleClient();
		Map<String, String> defaultParameters = app.getDefaultParameters();
		if (host != null)
		{
			defaultParameters.put("serverIp", host);
		}
		if (port != null)
		{
			defaultParameters.put("serverPort", port);
		}
		if (query != null)
		{
			defaultParameters.put("query", query);
			System.out.println("query:" + query);
		}
		QPRequest ret = app.runTest(defaultParameters);
		System.out.println(ret.toString());
	}

	public QPRequest runTest(Map<String, String> sc){
	    int ANET_PACKET_FLAG = 0x416e4574;
		int MAX_PACKET_HEADER = 32;
		query = sc.get("query");
		serverIp=sc.get("serverIp");
		serverPort=Integer.valueOf(sc.get("serverPort"));
        
        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(serverIp);
        sb.append(" Port: ").append(serverPort);
        sb.append("\n");
		
        //sr.sampleStart();
        byte[] bodyBuff = new byte[1024];
        QPRequest.Builder reqBuilder = QPRequest.newBuilder();
    	QPRequest ret = null;
        try {
        	reqBuilder.setOriginalKey(ByteString.copyFrom((query.getBytes("GBK"))));
        	CodedOutputStream outputBody = CodedOutputStream.newInstance(bodyBuff, 0, 1024);
        	QPRequest req = reqBuilder.build();
        	req.writeTo(outputBody);
            byte[] buffer = new byte[4*4 + MAX_PACKET_HEADER + req.getSerializedSize()];
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
			if (socketChannel == null)
			{
				socketChannel = SocketChannel.open();
				socketChannel.connect(new InetSocketAddress(serverIp, serverPort));
			}
			socketChannel.write(bs.asByteBuffer());
			ByteBuffer resBuffer = ByteBuffer.allocate(16);
			int bytesRead = socketChannel.read(resBuffer);
			if (bytesRead == 16)
			{
				ByteStream rbs = new ByteStream(resBuffer.array(), bytesRead);
				/*int flag = */rbs.popInt();
				/*int chid = */rbs.popInt();
				/*int pcode = */rbs.popInt();
				int dataLen = rbs.popInt();
				ByteBuffer dataBuffer = ByteBuffer.allocate(dataLen);
				bytesRead = socketChannel.read(dataBuffer);
				if (bytesRead == dataLen)
				{
					CodedInputStream in = CodedInputStream.newInstance(dataBuffer.array(),
							MAX_PACKET_HEADER, dataLen - MAX_PACKET_HEADER);
					//parse package
					ret = QPRequest.parseFrom(in);
					//System.out.println(ret.toString());
				}
			}
		} catch (Exception e) {
		}
		return ret;
	}
}
