import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Consulta {
    public static void main(String[] args) throws IOException {

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

        InetAddress direccionIP = InetAddress.getByName("127.0.0.1");

        // ############### PRIMERA FILA ##################
        Random random = new Random();
        short id = (short)random.nextInt(32768);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream(); // Es una clase en Java que proporciona un flujo de salida para almacenar datos en forma de bytes en una matriz de bytes en memoria. Es útil cuando necesitas trabajar con datos en memoria antes de almacenarlos o transmitirlos en otro lugar.
        DataOutputStream dataOutputStream = new DataOutputStream(arrayOutputStream); // Es una clase en Java que se utiliza para escribir datos primitivos en un flujo de salida en forma binaria. Es útil cuando se necesita almacenar datos estructurados en un archivo o transmitirlos a través de una red en un formato binario.

        // ############### SEGUNDA FILA ##################
        String QR = "0"; // Consulta (0) o una respuesta (1).
        String opCode = "0000"; // (0) Consulta estandart (Consulta), (1) consulta inversa (IQUERY), (2) una solicitud de estado del servidor (ESTADO).
        String AA = "0"; // AA.
        String TC = "0"; //TC.
        String RD = "1"; // RD.
        String RA = "0"; // RA.
        String Z  = "000"; // Z.
        String RDCODE = "0000"; // RDCODE.

        // Parseamos en una variable complea (banderas):
        String banderas = QR + opCode + AA + TC + RD + RA + Z + RDCODE;

        // Con las banderas bien configuradas creamos la variable de 16 bits:
        short banderasDeSolcititud = Short.parseShort(banderas, 2); // Representacion de "banderas" como un short en base 2.
        ByteBuffer byteBuffer = ByteBuffer.allocate(2).putShort(banderasDeSolcititud); // Buffer de bytes de longitud 2 (2 Bytes).
        byte[] arregloDeBufferDeBytes = byteBuffer.array(); // Matriz de bytes.

        // Debido a que estamos haciendo la petición del nombre de dominio, solo utilizamos "CuentaQD", ya que los demás se configuran cuando el servidor responde (igualmente se configuran a 0 las que no necesitemos).

        // ############### TERCERA FILA ##################
        short QDCOUNT = 1;

        // ############### CUARTA FILA ##################
        short ANCOUNT = 0;

        // ############### QUINTA FILA ##################
        short NSCOUNT = 0;

        // ############### SEXTA FILA ##################
        short ARCOUNT = 0;

        // Cargamos los datos primitivos en un flujo de salida en forma binaria, esto con el objetivo de transmitirlos a través de una red en un formato binario.
        dataOutputStream.writeShort(id);                // Primera fila.
        dataOutputStream.write(arregloDeBufferDeBytes); // Segunda fila.
        dataOutputStream.writeShort(QDCOUNT);           // Tercera fila.
        dataOutputStream.writeShort(ANCOUNT);           // Cuarta fila.
        dataOutputStream.writeShort(NSCOUNT);           // Quinta fila.
        dataOutputStream.writeShort(ARCOUNT);           // Sexta fila.

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
        String dominio = "www.google.com";
        String[] partesDeDominio = dominio.split("\\.");

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

        byte[] dnsFrame = arrayOutputStream.toByteArray();
        System.out.println("Enviando: " + dnsFrame.length + " bytes");
        for(int i = 0; i < dnsFrame.length; i++){
            System.out.println(String.format("%s", dnsFrame[i]) + " ");
        }

        DatagramSocket socket = new DatagramSocket();
        DatagramPacket dnsRqPacket = new DatagramPacket(dnsFrame, dnsFrame.length, direccionIP, 53); // Se prepara el paquete y se transporta a traves del puerto 53.
        socket.send(dnsRqPacket);
    }
}