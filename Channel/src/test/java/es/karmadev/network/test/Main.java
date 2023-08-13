package es.karmadev.network.test;

import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Throwable {
        NettyChannel server = new NettyChannel(8080);
        NettyChannel client = new NettyChannel("127.0.0.1", 8080);

        server.register((InputChannel) (channel, message) -> {
            System.out.println("Client >> Server: " + message.readUTF());
        });
        client.register((InputChannel) (channel, message) -> {
            System.out.println("Server >> Client: " + message.readUTF());
        });

        WritableMessage test = MessageConstructor.newOutMessage();
        test.writeUTF("Hello world!");
        test.setEncryption(true);

        client.write(test).whenComplete((response, error) -> {
            if (error != null) error.printStackTrace();
        });

        Scanner scanner = new Scanner(System.in);
        String command;
        while ((command = scanner.nextLine()) != null) {
            boolean fromServer = false;
            if (command.startsWith("--server ")) {
                command = command.replaceFirst("--server ", "");
                fromServer = true;
            }

            WritableMessage message = MessageConstructor.newOutMessage();
            message.writeUTF(command);
            message.setEncryption(true);

            if (fromServer) {
                server.write(message);
            } else {
                client.write(message);
            }
        }
    }
}
