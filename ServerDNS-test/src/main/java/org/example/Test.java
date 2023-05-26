package org.example;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Test {
    public static void main(String[] args) throws IOException {

        // The server ip is the localhost:
        InetAddress serverAddress =  InetAddress.getByName("127.0.0.1");

        // DNS default port:
        int port = 53;

        // Start DNS server:
        try {
            byte[] message = new byte[512];
            byte[] responseData = new byte[512];
            DatagramPacket requestPacket = new DatagramPacket(message, message.length);
            DatagramPacket responsePacket = new DatagramPacket(message, message.length);
            DatagramSocket socket = new DatagramSocket(port, serverAddress);
            while (true) {

                // Wait for request:
                socket.receive(requestPacket);
                System.out.println("\n\nReceived: \t\t" + requestPacket.getLength() + " bytes");
                InetAddress clientAddress = requestPacket.getAddress(); // Obtiene la dirección IP del cliente
                String clientIP = clientAddress.getHostAddress(); // Convierte la dirección IP a una representación de String
                System.out.println("Client IPv4: \t" + clientIP);

                // Luego mandamos de nuevo el mensaje con la estructura del protocolo DNS, pero esta ves la respuesta:
                responseData = buildResponse(message);
                if(responseData.length != 0){
                    System.out.println("\n\nSended: " + responseData.length + " bytes");
                    responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, port);
                    socket.send(responsePacket);
                    // Se actualiza estado de variables para esperar una nueva solicitud
                    socket.close();
                    requestPacket = new DatagramPacket(message, message.length);
                    responsePacket = new DatagramPacket(message, message.length);
                    socket = new DatagramSocket(port, serverAddress);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    static byte[] buildResponse(byte[] response) throws IOException{
        byte[] responseData = new byte[0]; // Buffer que va a ir cargando todos los campos del mensaje DNS.

        /*
                 1. Solicitud del DNS:
                    +------------------------------+
                    |          Encabezado          |
                    +------------------------------+
                    |           Pregunta           |    la pregunta para el servidor de nombres.
                    +------------------------------+
                    |           Respuesta          |    RRs respondiendo pregunta.
                    +------------------------------+
                    |           Autoridad          |    RR apuntando hacia un autoridad.
                    +------------------------------+
                    |           Adicional          |    RR con información adicional.
                    +------------------------------+
         */

        // Aca en adelante solo queremos comprobar que se reciba correctamente el paquete en el cual viene la solicitud del DNS:
        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(response));

        // ################## Primera fila ##################
        short ID = dataInputStream.readShort(); // Mueve el buffer 16 bits a la derecha, o lo que significa que agarra la primera fila (ID).

        // ################## Segunda fila ##################
        short flags = dataInputStream.readByte(); // Mueve el buffer 8 bits a la derecha, o lo que significa que agarra el primer byte de la segunda fila (QR, Opcode, AA, TC, RD).
        int QR = (flags & 0b10000000) >>> 7; // Aca en adelante queremos agarrar las banderas, por lo que utilizamos una operacion AND que nos permite especificar el bit dentro de byte que queremos separar y guardar en la variable.
        String QResponse = "1";
        int opCode = ( flags & 0b01111000) >>> 3;
        String opCodeResponse = "0000";
        int AA = ( flags & 0b00000100) >>> 2;
        String AAResponse = "1";
        int TC = ( flags & 0b00000010) >>> 1;
        String TCResponse = "0";
        int RD = flags & 0b00000001;
        String RDResponse = "0";
        flags = dataInputStream.readByte(); // Mueve el buffer 8 bits a la derecha, lo que significa el byte restante de la fila 2 (RA, Z, RCODE).
        int RA = (flags & 0b10000000) >>> 7;
        String RAResponse = "0";
        int Z = ( flags & 0b01110000) >>> 4;
        String ZResponse = "000";
        int RCODE = flags & 0b00001111;
        String RCODEResponse = "0000";
        String flagsResponseAux = QResponse + opCodeResponse + AAResponse + TCResponse + RDResponse + RAResponse + ZResponse + RCODEResponse;
        int decimal = Integer.parseInt(flagsResponseAux, 2);
        short flagsResponse = (short) decimal;
        ByteBuffer byteBuffer = ByteBuffer.allocate(2).putShort(flagsResponse); // Buffer de bytes de longitud 2 (2 Bytes).
        byte[] arregloDeBufferDeBytes = byteBuffer .array(); // Matriz de bytes.

        short QDCOUNT = dataInputStream.readShort(); // Fila 3
        short QDCOUNTResponse = 1;

        short ANCOUNT = dataInputStream.readShort(); // Fila 4
        int anCount = 1;  // Número deseado de registros de respuesta

        short NSCOUNT = dataInputStream.readShort(); // Fila 5
        int nsCount = 0;

        short ARCOUNT = dataInputStream.readShort(); // Fila 6
        int arCount = 0;  // Número deseado de registros adicionales

        String QNAME = "";
        StringBuilder domainBuilder = new StringBuilder();
        int recLen;
        while ((recLen = dataInputStream.readByte()) > 0) {
            byte[] record = new byte[recLen];
            for (int i = 0; i < recLen; i++) {
                record[i] = dataInputStream.readByte();
            }
            String label = new String(record, StandardCharsets.UTF_8);
            domainBuilder.append(label).append(".");
        }

// Eliminar el último punto sobrante
        if (domainBuilder.length() > 0) {
            domainBuilder.setLength(domainBuilder.length() - 1);
        }
        String domain = domainBuilder.toString();
        System.out.println("Domain: \t\t" + domain);
        short QTYPE = dataInputStream.readShort();
        short QCLASS = dataInputStream.readShort();

        // Cargamos el Master File:
        String zoneFile1 = "src/Zones/com.zone"; // .com
        String zoneFile2 = "src/Zones/edu.co.zone"; // .edu.co
        List<String> zoneFiles = new ArrayList<>();
        zoneFiles.add(zoneFile1);
        zoneFiles.add(zoneFile2);
        String ipAddress = null;
        int registerLength = 0;
        try{
            for(String zoneFile: zoneFiles){
                Master master = new Master(zoneFile);
                Record record;
                while ((record = master.nextRecord()) != null) {
                    if (record.getType() == Type.A && record.getName().toString().equals(domain + ".")) {
                        registerLength++;
                        ipAddress = record.rdataToString();
                        System.out.println("Domain IPv4: \t" + record.rdataToString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        anCount = registerLength;
        if(ipAddress != null){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            /*

            2. Encabezado:

                                                    1  1  1  1  1  1
                      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                Identificación                 | 2 bytes (±32768) genera cualquier tipo de consulta. Este identificador se copia a la respuesta correspondiente y solicitante puede utilizarlo para cotejar las respuestas con las consultas pendientes.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |  |           |  |  |  |  |        |           | QR = Campo de un bit que especifica si este mensaje es una consulta (0) o una respuesta (1).                                                                                                                                                         ┐
                    |  |           |  |  |  |  |        |           | Opcode = Campo de cuatro bits que especifica el tipo de consulta en este mensaje; (0) Consulta estandart (Consulta), (1) consulta inversa (IQUERY), (2) una solicitud de estado del servidor (ESTADO).                                               |
                    |  |           |  |  |  |  |        |           | AA = Respuesta autorizada, este bit es válido en las respuestas, y especifica que el servidor de nombres que responde es un autoridad para el nombre de dominio en la sección de cuestión.                                                           |
                    |  |           |  |  |  |  |        |           | TC = especifica que este mensaje fue truncado debido a una longitud mayor que la permitida en el canal de transmisión.                                                                                                                               |
                    |QR|  Opcode   |AA|TC|RD|RA|   Z    |   RCODE   | RD = Recursividad deseada - este bit se puede establecer en una consulta y se copia en la respuesta. Si se establece RD, dirige el servidor de nombres para realizar la consulta de forma recursiva. El soporte de consulta recursiva es opcional.   ├------» 2 bytes
                    |  |           |  |  |  |  |        |           | RA = Recursividad disponible - esto se establece o se borra en un respuesta, y denota si el soporte de consulta recursiva es disponible en el servidor de nombres.                                                                                   |
                    |  |           |  |  |  |  |        |           | Z = Reservado para uso futuro. Debe ser cero en todas las consultas y respuestas.                                                                                                                                                                    |
                    |  |           |  |  |  |  |        |           | RCODE = Código de respuesta - este campo de 4 bits se establece como parte de respuestas.                                                                                                                                                            ┘
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                    QDCOUNT                    | Un entero de 16 bits sin signo que especifica el número de entradas en la sección de preguntas.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                    ANCOUNT                    | Un entero de 16 bits sin signo que especifica el número de registros de recursos en la sección de respuestas.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                    NSCOUNT                    | Un entero de 16 bits sin signo que especifica el número de nombre.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                    ARCOUNT                    | Un entero de 16 bits sin signo que especifica el número de registros de recursos en la sección de registros adicionales.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+

                    // El orden de transmisión del encabezado y los datos describtos se resuelve a nivel Octeto.

                    -> Byte  = 1 Byte
                    -> short = 2 Bytes
             */

            // Cargamos los datos primitivos en un flujo de salida en forma binaria, esto con el objetivo de transmitirlos a través de una red en un formato binario.
            dataOutputStream.writeShort(ID);                        // Primera fila.
            dataOutputStream.write(arregloDeBufferDeBytes);                  // Segunda fila.
            dataOutputStream.writeShort(QDCOUNTResponse);           // Tercera fila.
            dataOutputStream.write((anCount >> 8) & 0xFF);       // Byte más significativo
            dataOutputStream.write(anCount & 0xFF);              // Byte menos significativo.
            dataOutputStream.write((nsCount >> 8) & 0xFF);       // Byte más significativo      // Quinta fila
            dataOutputStream.write(nsCount & 0xFF);              // Byte menos significativo    // Quinta fila
            dataOutputStream.write((arCount >> 8) & 0xFF);       // Byte más significativo
            dataOutputStream.write(arCount & 0xFF);              // Byte menos significativo
             /*
             3. Pasamos a la sección de preguntas, la cual se utiliza para llevar la información de la pregunta en la mayoría de las consultas.

                                                    1  1  1  1  1  1
                      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                                               | Un nombre de dominio representado como una secuencia de etiquetas, donde cada etiqueta consiste en un octeto de longitud seguido de eso
                    /                     QNAME                     / número de octetos. El nombre de dominio termina con el octeto de longitud cero para la etiqueta nula de la raíz.
                    /                                               /
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     QTYPE                     | Un código de dos octetos que especifica el tipo de consulta. Los valores de este campo incluyen todos los códigos válidos para un Campo TIPO, junto con algunos códigos más generales que puede coincidir con más de un tipo de RR.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     QCLASS                    | Un código de dos octetos que especifica la clase de la consulta. Por ejemplo, el campo QCLASS está IN para Internet.
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
             */

                // ############### PRIMERA FILA ##################
                String[] partesDeDominio = domain.split("\\.");

                for(int i = 0; i < partesDeDominio.length; i++){
                    byte[] bytesDeDominio = partesDeDominio[i].getBytes(StandardCharsets.UTF_8); // UTF-8 utiliza una representación de longitud variable para los diferentes caracteres, lo que significa que algunos caracteres se representan con un solo byte, mientras que otros pueden requerir múltiples bytes. Esto permite una eficiente representación de texto en diferentes idiomas, minimizando el tamaño del archivo o los datos transmitidos.
                    dataOutputStream.writeByte(bytesDeDominio.length);
                    dataOutputStream.write(bytesDeDominio);
                }
                dataOutputStream.writeByte(0); // No más partes.

                // ############### SEGUNDA FILA ##################

                // Para este caso en concreto utilizamos el tipo 1, el cual es una sola dirección de host (Es decir no se pueden ejecutar solicitudes desde diferentes host al mismo tiempo).
                dataOutputStream.writeShort(1);

                // ############### TERCERA FILA ##################

                // Utilizamos IN = 1 (Para internet) para este ejemplo:
                dataOutputStream.writeShort(1);

                /*
            4. Respuesta:
                                                    1  1  1  1  1  1
                      0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                                               |
                    /                                               /
                    /                      NAME                     /
                    |                                               |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                      TYPE                     |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                     CLASS                     |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                      TTL                      |
                    |                                               |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    |                   RDLENGTH                    |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
                    /                     RDATA                     /
                    /                                               /
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
             */
            // Ahora si la respuesta
            // ############ Primera fila ############:
            String[] labels = domain.split("\\.");
            for(int i = 0; i < labels.length; i++){
                byte[] bytesDeDominio = labels[i].getBytes(StandardCharsets.UTF_8); // UTF-8 utiliza una representación de longitud variable para los diferentes caracteres, lo que significa que algunos caracteres se representan con un solo byte, mientras que otros pueden requerir múltiples bytes. Esto permite una eficiente representación de texto en diferentes idiomas, minimizando el tamaño del archivo o los datos transmitidos.
                dataOutputStream.writeByte(bytesDeDominio.length);
                dataOutputStream.write(bytesDeDominio);
            }

            dataOutputStream.writeByte(0);  // Terminador de etiquetas

            // ############ Segunda fila ############
            int type = 1; // Tipo  A
            dataOutputStream.writeShort(type);

            // ############ Tercera fila ############
            int dnsClass = 1;
            dataOutputStream.writeShort(dnsClass);

            // ############ Cuarta fila ############
            int ttl = 90; // Valor TTL en segundos 1:30
            dataOutputStream.writeInt(ttl);


            // ############ Quinta fila ############
            int rdLength = 4;  // Longitud de datos de 4 bytes (00000000.00000000.00000000.00000000)
            dataOutputStream.writeShort(rdLength);

            // ############ Sexta fila ############
            String[] octets = ipAddress.split("\\.");
            byte[] rdata = new byte[4];

            for (int i = 0; i < octets.length; i++) {
                int octetValue = Integer.parseInt(octets[i]);
                rdata[i] = (byte) octetValue;
            }
            dataOutputStream.write(rdata); // RDATA

            /*

            5. Compresion de mensaje:
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
                    | 1  1|                OFFSET                   |
                    +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
             */

            responseData = byteArrayOutputStream.toByteArray();// el mensaje de respuesta DNS como un arreglo de bytes
        }
        return responseData;
    };
}
