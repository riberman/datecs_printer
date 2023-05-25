package com.rezins.datecs_printer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONException;

/** DatecsPrinterPlugin */
public class DatecsPrinterPlugin implements FlutterPlugin, MethodCallHandler {

  private MethodChannel channel;

  private Context mContext;

  private Printer mPrinter;
  private ProtocolAdapter mProtocolAdapter;
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothSocket mmSocket;
  private BluetoothDevice mmDevice;
  private OutputStream mmOutputStream;
  private InputStream mmInputStream;

  private boolean isConnect = false;

  private final ProtocolAdapter.ChannelListener mChannelListener = new ProtocolAdapter.ChannelListener(){
    @Override
    public void onReadEncryptedCard() {
      // TODO: onReadEncryptedCard
    }

    @Override
    public void onReadCard() {
      // TODO: onReadCard
    }

    @Override
    public void onReadBarcode() {
      // TODO: onReadBarcode
    }

    @Override
    public void onPaperReady(boolean state) {
      if (state) {
      } else {
        try {
          disconnect();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    @Override
    public void onOverHeated(boolean state) {
      if (state) {
      }
    }
    // 6ca1a08e05c9439bbb6c2825ae7fdec4
    @Override
    public void onLowBattery(boolean state) {
      if (state) {
      }
    }
  };

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "datecs_printer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getListBluetoothDevice")){

      // Get the local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      ArrayList<Map<String, String>> devices = new ArrayList<Map<String, String>>();
      if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
          Map<String, String> list = new HashMap<String, String>();
          list.put("name", device.getName());
          list.put("address", device.getAddress());
          devices.add(list);
        }
        result.success(devices);


      }else{

        result.error("Error 101", "Error while get list bluetooth device","");
      }

    }else if (call.method.equals("connectBluetooth")){
      String address = call.argument("address");
      try{
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
          isConnect = connect(address);
        }
        result.success(isConnect);
      }catch(IOException e){
        result.success(isConnect);
      }catch(Exception e){
        result.success(isConnect);
      }

    }else if(call.method.equals("disconnectBluetooth")){
      try{
        disconnect();
        result.success(true);
      }catch(IOException e){
        result.success(false);
      }
    }else if(call.method.equals("testPrint")){
      try {
        mPrinter.printSelfTest();
        mPrinter.flush();
        result.success(true);
      } catch (Exception e) {
        result.success(false);
      }
    }else if(call.method.equals("printText")){
      String charset = "ISO-8859-1";
      List<String> args = call.argument("args");
      try {
        mPrinter.reset();
        for(int i = 0; i < args.size(); i++){
          if(args.get(i).contains("feed%20")){
            String[] split = args.get(i).split("%20");
            int feed = Integer.parseInt(split[1]);
            mPrinter.feedPaper(feed);
          }else if(args.get(i).contains("img%2021")){
            String[] split = args.get(i).split("%2021");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 300, 300, true);
              final int[] argb = new int[300 * 300];
              resized.getPixels(argb, 0, 300, 0, 0, 300, 300);
              resized.recycle();

              mPrinter.printImage(argb, 300, 300, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 300, 300, true);
              final int[] argb = new int[300 * 300];
              resized.getPixels(argb, 0, 300, 0, 0, 300, 300);
              resized.recycle();
              mPrinter.printImage(argb, 300, 300, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("img%danfe")){
            String[] split = args.get(i).split("%danfe");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1250, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1250, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("img%boleto")){
            String[] split = args.get(i).split("%boleto");
            String img = split[1];
            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1400, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, 840, 1400, true);
              // decodedByte.recycle();
              final int width = resized.getWidth();
              final int height = resized.getHeight();
              final int[] argb = new int[width * height];
              resized.getPixels(argb, 0, width, 0, 0, width, height);
              resized.recycle();

              mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_CENTER, true);
            }
          }else if(args.get(i).contains("print%teste")){
            String jsonString = "{\"data\":{\"id\":\"0018cb00-cc97-11ed-8bce-0242ac1c0002\",\"company_group_id\":\"5e7c3ea1-bbf8-4254-8d7d-8b730d7548ac\",\"created_at\":\"2023-03-27T00:00:00.000000Z\",\"updated_at\":\"2023-03-29T15:59:37.000000Z\",\"ref\":\"495200\",\"xml\":true,\"serie\":\"20\",\"numero\":\"905\",\"chave\":\"41230308168798000197550200000009051248576160\",\"protocolo\":\"141230000199289\",\"natOp\":\"VENDA DE MERCADORIA\",\"dhEmi\":\"26/03/2023\",\"dSaiEnt\":\"--/--/--\",\"hSaiEnt\":\"--:--\",\"bcIcms\":\"\",\"vIcms\":\"\",\"bcIcmsSt\":\"\",\"vIcmsSt\":\"\",\"ttProdutos\":\"36.00\",\"vFrete\":\"0.00\",\"vSeguro\":\"0.00\",\"vDesc\":\"0.00\",\"vOutro\":\"0.00\",\"vIPI\":\"4.32\",\"ttNota\":\"40.32\",\"adicionais\":\"teste DECRETO 001 tesde DECRETO 002  teste OBSERVACAO 001 teste OBSERVACAO 0002  VLR IPI R$4,32 PERMITE O APROVEITAMENTO DO CREDITO DE ICMS NO VALOR DE R$ 0,96 CORRESPONDENTE A ALIQUOTA DE 2,67% NOS TERMOS DO ART. 23 DA LC 123/2006. NF Ref. Pedido No. 0000000950\",\"emitNome\":\"NOME DA EMPRESA FANTASIA\",\"emitEnd\":\"RUA GETULIO VARGASS, 195 Centro CEP: 85010-280 GUARAPUAVA-PR\",\"emitCNPJ\":\"08.168.798/0001-97\",\"emitIe\":\"9041463667\",\"emitTel\":\"42369-5878\",\"destRazao\":\"NF-E EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL\",\"destEnd\":\"RUA FREI CANECA, 1713 SANTAN CEP: 85070-170 GUARAPUAVA-PR\",\"destIe\":\"2\",\"produtos\":[{\"cProd\":\"000002\",\"cEAN\":\"SEM GTIN\",\"xProd\":\"NOTA FISCAL EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL\",\"NCM\":\"40081100\",\"CFOP\":\"5102\",\"uCom\":\"UN\",\"qCom\":\"1.0000\",\"vUnCom\":\"36.0000000000\",\"vProd\":\"36.00\",\"cEANTrib\":\"SEM GTIN\",\"uTrib\":\"UN\",\"qTrib\":\"1.0000\",\"vUnTrib\":\"36.0000000000\",\"indTot\":\"1\",\"nItem\":\"1\"}]}}";

            String emitNome = "";
            String emitCNPJ = "";
            String emitIe = "";
            String emitTel = "";
            String emitEnd = "";
            String numero = "";
            String serie = "";
            String chave = "";
            String protocolo = "";
            String natOp = "";
            String destRazao = "";
            String destEnd = "";
            String detDocument = "";
            String destIe = "";
            String emissao = "";
            String dSaiEnt = "";
            String hSaiEnt = "";
            String bcIcms = "";
            String vIcms = "";
            String bcIcmsSt = "";
            String vIcmsSt = "";
            String ttProdutos = "";
            String vFrete = "";
            String vSeguro = "";
            String vDesc = "";
            String vOutro = "";
            String vIPI = "";
            String ttNota = "";
            String adicionais = "";
            try {
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONObject invoice = jsonObject.getJSONObject("data");
            JSONArray produtosArray = invoice.getJSONArray("produtos");

            emitNome = invoice.getString("emitNome");
            emitCNPJ = invoice.getString("emitCNPJ");
            emitIe = invoice.getString("emitIe");
            emitTel = invoice.getString("emitTel");
            emitEnd = invoice.getString("emitEnd");
            numero = invoice.getString("numero");
            serie = invoice.getString("serie");
            chave = invoice.getString("chave");
            protocolo = invoice.getString("protocolo");
            natOp = invoice.getString("natOp");
            destRazao = invoice.getString("destRazao");
            destEnd = invoice.getString("destEnd");
            destDocument = invoice.getString("destDocument");
            destIe = invoice.getString("destIe");
            emissao = invoice.getString("dhEmi");
            dSaiEnt = invoice.getString("dSaiEnt");
            hSaiEnt = invoice.getString("hSaiEnt");
            bcIcms = invoice.getString("bcIcms");
            bcIcms = invoice.getString("vIcms");
            bcIcmsSt = invoice.getString("bcIcmsSt");
            vIcmsSt = invoice.getString("vIcmsSt");
            ttProdutos = invoice.getString("ttProdutos");
            vFrete = invoice.getString("vFrete");
            vSeguro = invoice.getString("vSeguro");
            vDesc = invoice.getString("vDesc");
            vOutro = invoice.getString("vOutro");
            vIPI = invoice.getString("vIPI");
            ttNota = invoice.getString("ttNota");
            adicionais = invoice.getString("adicionais");
            
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectPageMode();
            mPrinter.setPageRegion(0, 0, 650, 220, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 650, 100, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(5, 5);

            mPrinter.printTaggedText("{reset}Recebemos de " + emitNome
                            + " os produtos constantes da nota fiscal indicada ao lado.{br}");
            mPrinter.setPageXY(0, 105);
            mPrinter.printTaggedText("{reset}{center}{b}DATA E HORA DO RECEBIMENTO{br}");
            mPrinter.drawPageRectangle(0, 100, 650, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 100, 650, 120, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(650, 0, 150, 220, Printer.PAGE_LEFT);
            mPrinter.printTaggedText("{reset}{br}{center}{h} 1 - Saida {br}{center}NF-e{br}{center}N " + numero + "{br}{center}Serie " + serie, "UTF-8");
            mPrinter.drawPageFrame(0, 0, 150, 220, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(0, 220, 800, 120, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}IDENTIFICACAO E ASSINATURA DO RECEBEDOR{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 120, Printer.FILL_BLACK, 2);

            mPrinter.setPageRegion(0, 340, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{b}--------------------------------------------------------------------{br}");
            int y = 372;

            mPrinter.setPageRegion(0, y, 650, 280, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.drawPageFrame(0, 0, 650, 280, Printer.FILL_BLACK, 2);

            // if (logo != null) {
            //   Bitmap resized = Bitmap.createScaledBitmap(logo, 200, 144, true);
            //   final int width = resized.getWidth();
            //   final int height = resized.getHeight();
            //   final int[] argb = new int[width * height];
            //   resized.getPixels(argb, 0, width, 0, 0, width, height);
            //   resized.recycle();

            //   mPrinter.printCompressedImage(argb, width, height, Printer.ALIGN_LEFT, true);
            // }

            mPrinter.setPageXY(0, 30);
            mPrinter.printTaggedText("{reset}{br}{right}{h}{w}DANFE SIMPLIFICADO{br}{center}{/w}{s}{right}Documento Auxiliar de Nota Fiscal Eletronica{br}");
            mPrinter.setPageXY(0, 130);
            mPrinter.printTaggedText("{reset}{br}{b}" + emitNome + "{/b}{br}" + emitCNPJ + " " + emitIe + " " + emitTel + "{br}" + emitEnd + "{br}");

            mPrinter.setPageRegion(650, y, 150, 280, Printer.PAGE_LEFT);
            mPrinter.printTaggedText("{reset}{br}{center}{h} 1 - Saida {br}{center}NF-e{br}{center}N " + numero + "{br}{center}Serie " + serie, "UTF-8");
            mPrinter.drawPageFrame(0, 0, 150, 280, Printer.FILL_BLACK, 2);
            y += 280;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}CHAVE DE ACESSO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            String s = chave.replaceAll("(....(?!\\z))", "$1 ");
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + s + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 800, 100, Printer.PAGE_LEFT);
            mPrinter.setBarcode(Printer.ALIGN_CENTER, true, 2, Printer.HRI_BELOW, 100);
            mPrinter.printBarcode(Printer.BARCODE_CODE128AUTO, chave);

            y += 100 + 15;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}PROTOCOLO DE AUTORIZACAO DE USO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + protocolo + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 800, 64, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}NATUREZA DA OPERACAO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 64, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{center}" + natOp + "{br}");
            y += 64 + 15;

            mPrinter.setPageRegion(0, y, 650, 220, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}DESTINATARIO{br}");
            mPrinter.drawPageRectangle(0, 0, 650, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 650, 220, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);

            mPrinter.printTaggedText("{reset}{left}{b}" + destRazao + "{/b}{br}" + destEnd + "{br}CNPJ/CPF: " + destDocument + "{br}I.E.:" + destIe + "{br}");

            mPrinter.setPageRegion(650, y, 150, 73, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}EMISSAO{br}{center}" + emissao + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 73, Printer.FILL_BLACK, 2);
            mPrinter.setPageRegion(650, y + 73, 150, 73, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}SAIDA{br}{center}" + dSaiEnt + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 73, Printer.FILL_BLACK, 2);
            mPrinter.setPageRegion(650, y + 73 + 73, 150, 74, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 10);
            mPrinter.printTaggedText("{reset}{center}HORA{br}{center}" + hSaiEnt + "{br}");
            mPrinter.drawPageFrame(0, 0, 150, 74, Printer.FILL_BLACK, 2);
            y += 220 + 15;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}FATURAS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            mPrinter.printPage();
            mPrinter.flush();
            mPrinter.reset();
            mPrinter.selectStandardMode();
            mPrinter.selectPageMode();
            mPrinter.flush();
            y = 0;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}IMPOSTO{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            mPrinter.setPageRegion(0, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}B.C.ICMS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + bcIcms + "{br}");
            int x = 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.ICMS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIcms + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}B.C.ICMS.ST{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + bcIcmsSt + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 150, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 150, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 150, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.ICMS.ST{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIcmsSt + "{br}");
            x += 150;

            mPrinter.setPageRegion(x, y, 200, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 200, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 200, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}TOTAL PRODUTOS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + ttProdutos + "{br}");
            x = 0;
            y += 74;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.FRETE{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vFrete + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.SEGURO{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vSeguro + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}DESCONTO{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vDesc + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}OUTROS{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vOutros + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 120, 74, Printer.PAGE_LEFT);
            mPrinter.drawPageFrame(0, 0, 120, 30, Printer.FILL_BLACK, 2);
            mPrinter.drawPageFrame(0, 30, 120, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.IPI{br}");
            mPrinter.setPageXY(0, 48);
            mPrinter.printTaggedText("{reset}{right}" + vIpi + "{br}");
            x += 120;

            mPrinter.setPageRegion(x, y, 200, 74, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}TOTAL GERAL{br}");
            mPrinter.drawPageRectangle(0, 0, 200, 30, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 30, 200, 44, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 35);
            mPrinter.printTaggedText("{reset}{right}{h}" + ttNota + "{br}");
            x = 0;
            y += 74 + 15;

            mPrinter.setPageRegion(0, y, 800, 32, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}PRODUTOS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            y += 32;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}COD{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 300, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}DESCRICAO{br}");
            mPrinter.drawPageFrame(0, 0, 300, 30, Printer.FILL_BLACK, 2);
            x += 300;

            mPrinter.setPageRegion(x, y, 100, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}NCM/SH{br}");
            mPrinter.drawPageFrame(0, 0, 100, 30, Printer.FILL_BLACK, 2);
            x += 100;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}CST{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}CFOP{br}");
            mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
            x += 50;

            mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}UN{br}");
            mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
            x += 40;

            mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}QTDE{br}");
            mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
            x += 50;

            mPrinter.setPageRegion(x, y, 80, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.UNIT.{br}");
            mPrinter.drawPageFrame(0, 0, 80, 30, Printer.FILL_BLACK, 2);
            x += 80;

            mPrinter.setPageRegion(x, y, 105, 30, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}V.TOTAL{br}");
            mPrinter.drawPageFrame(0, 0, 105, 30, Printer.FILL_BLACK, 2);
            y += 30;

            for (int i = 0; i < produtosArray.length(); i++) {
                JSONObject produto = produtosArray.getJSONObject(i);
                
                String cProd = produto.getString("xProd");
                System.out.println(cProd);

                 x = 0;
                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("cProd") + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                printer.setPageRegion(x, y, 300, 30, Printer.PAGE_LEFT);
                printer.setPageXY(0, 5);
                printer.printTaggedText("{reset}{left}{s}" + produto.getString("xProd")  + "{br}");
                printer.drawPageFrame(0, 0, 300, 30, Printer.FILL_BLACK, 2);
                x += 300;

                mPrinter.setPageRegion(x, y, 100, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("NCM") + "{br}");
                mPrinter.drawPageFrame(0, 0, 100, 30, Printer.FILL_BLACK, 2);
                x += 100;

                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + products.get(i).getCst() + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("CFOP") + "{br}");
                mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
                x += 50;

                mPrinter.setPageRegion(x, y, 40, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("uTrib")  + "{br}");
                mPrinter.drawPageFrame(0, 0, 40, 30, Printer.FILL_BLACK, 2);
                x += 40;

                mPrinter.setPageRegion(x, y, 50, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{center}{s}" + produto.getString("qCom") + "{br}");
                mPrinter.drawPageFrame(0, 0, 50, 30, Printer.FILL_BLACK, 2);
                x += 50;

                mPrinter.setPageRegion(x, y, 80, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{right}{s}" + produto.getString("vUnCom") + "{br}");
                mPrinter.drawPageFrame(0, 0, 80, 30, Printer.FILL_BLACK, 2);
                x += 80;

                mPrinter.setPageRegion(x, y, 105, 30, Printer.PAGE_LEFT);
                mPrinter.setPageXY(0, 5);
                mPrinter.printTaggedText("{reset}{right}{s}" + produto.getString("vProd") + "{br}");
                mPrinter.drawPageFrame(0, 0, 105, 30, Printer.FILL_BLACK, 2);
                y += 30;
            }

            mPrinter.setPageRegion(0, y, 800, 220, Printer.PAGE_LEFT);
            mPrinter.setPageXY(0, 5);
            mPrinter.printTaggedText("{reset}{center}{b}DADOS ADICIONAIS{br}");
            mPrinter.drawPageRectangle(0, 0, 800, 32, Printer.FILL_INVERTED);
            mPrinter.drawPageFrame(0, 0, 800, 220, Printer.FILL_BLACK, 2);
            mPrinter.setPageXY(0, 37);
            mPrinter.printTaggedText("{reset}{left}" + adicionais + "{br}");

            mPrinter.printPage();
            mPrinter.selectStandardMode();
            mPrinter.feedPaper(110);
            mPrinter.flush();
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }else{
            mPrinter.printTaggedText(args.get(i));
          }
        }
        mPrinter.flush();
        result.success(true);
      } catch (IOException e) {
        result.success(false);
      } catch (NullPointerException e){
        result.success(false);
      }
    }else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public boolean connect(String address) throws IOException {
    try{
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      BluetoothDevice device = adapter.getRemoteDevice(address);
      mmDevice = device;
      UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
      mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
      mmSocket.connect();
      mmOutputStream = mmSocket.getOutputStream();
      mmInputStream = mmSocket.getInputStream();
      initializePrinter(mmInputStream, mmOutputStream);
      return true;
    }catch(Exception error){
      throw error;
    }
  }

  protected void initializePrinter(InputStream inputStream, OutputStream outputStream) throws IOException {
    mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
    if (mProtocolAdapter.isProtocolEnabled()) {
      final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);

      // Create new event pulling thread
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            try {
              channel.pullEvent();
            } catch (IOException e) {

              break;
            }
          }
        }
      }).start();
      mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
    } else {
      mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
    }

  }

  public void disconnect() throws IOException{
    try {
      mmSocket.close();

      if (mPrinter != null) {
        mPrinter.release();
      }

      if (mProtocolAdapter != null) {
        mProtocolAdapter.release();
      }


    } catch (Exception e) {

    }
  }

}
