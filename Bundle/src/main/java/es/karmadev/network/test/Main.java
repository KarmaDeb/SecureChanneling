package es.karmadev.network.test;

import es.karmadev.api.network.channel.NetChannel;
import es.karmadev.api.network.channel.handler.InputChannel;
import es.karmadev.api.network.message.ReadOnlyMessage;
import es.karmadev.api.network.message.WritableMessage;
import es.karmadev.network.channel.NettyChannel;
import es.karmadev.network.message.MessageConstructor;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        String command = "";

        NettyChannel channel = null;
        while ((command = scanner.nextLine()) != null) {
            if (command.equals("exit")) {
                break;
            }

            if (channel == null) {
                if (command.equals("server")) {
                    channel = new NettyChannel(8080);
                    channel.register(new InputChannel() {
                        @Override
                        public void receive(NetChannel channel, ReadOnlyMessage message) {
                            System.out.println(" >> " + message.readUTF());
                        }
                    });
                } else {
                    if (command.equals("client")) {
                        channel = new NettyChannel("127.0.0.1", 8080);
                        channel.register(new InputChannel() {
                            @Override
                            public void receive(NetChannel channel, ReadOnlyMessage message) {
                                System.out.println(" >> " + message.readUTF());
                            }
                        });
                    }
                }
            } else {
                WritableMessage message = MessageConstructor.newOutMessage();
                message.writeUTF(command);
                message.setEncryption(true);

                channel.write(message);
            }
        }
    }
}
