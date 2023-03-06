package com.example.socialcompass.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.example.socialcompass.model.api.API;
import com.example.socialcompass.model.friend.Friend;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Helper class
 * Will execute the script file
 */
class ExecuteShellCommand {
    public static void executeCommand(String filePath) {
        try {
            // set permission to the file to executable
            File file = new File(filePath);
            file.setExecutable(true);
            // execute the file
            Process process = Runtime.getRuntime().exec(filePath);
            // print the result
            BufferedReader read = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            while (read.ready()) {
                System.out.println(read.readLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class APITest extends TestCase {
    private API api;
    private String privateCode;
    private Friend testFriend;

    @Before
    public void setup() {
        api = API.provide();
        privateCode = "some random string here";
        testFriend = new Friend("the-world", "The world", (float) -25.326356, (float) 33.25622);

        // put the test friend to server
        String script = "./src/test/java/com/example/socialcompass/model/create_script";
        ExecuteShellCommand.executeCommand(script);
    }

    @After
    public void dismantle(){
        // remove the test friend from server
        String script = "./src/test/java/com/example/socialcompass/model/delete_script";
        ExecuteShellCommand.executeCommand(script);
    }


    @Test
    public void testGetFriend() {
        Friend friend = api.getFriend(testFriend.publicCode);
//        assertEquals(testFriend.publicCode, friend.publicCode);
    }
}
