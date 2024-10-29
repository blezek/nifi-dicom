package com.blezek.nifi.dicom;

import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSessionFactory;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DICOMTransferTest {

    private TestRunner listenDICOM;
    private ListenDICOM listenProcessor;
    private int port;
    private TestRunner putDICOM;

    @BeforeEach
    public void setup() throws IOException {
        port = findAvailablePort();
        putDICOM = TestRunners.newTestRunner(PutDICOM.class);
        putDICOM.setProperty(PutDICOM.CALLED_AE_TITLE, "nifi");
        putDICOM.setProperty(PutDICOM.CALLING_AE_TITLE, "nifi-send");
        putDICOM.setProperty(PutDICOM.DICOM_HOSTNAME, "localhost");
        putDICOM.setProperty(PutDICOM.DICOM_PORT, Integer.toString(port));
        putDICOM.assertValid();

        // Configure the listener
        listenProcessor = new ListenDICOM();
        listenDICOM = TestRunners.newTestRunner(listenProcessor);
        listenDICOM.setProperty(ListenDICOM.AE_TITLE, "*");
        listenDICOM.setProperty(ListenDICOM.DICOM_PORT, Integer.toString(port));
        listenDICOM.assertValid();
    }

    @AfterEach
    public void tearDown() {
        listenDICOM.shutdown();
        putDICOM.shutdown();
    }

    @Test
    public void send() throws IOException, GeneralSecurityException {
        // Start the listener
        ProcessSessionFactory processSession = listenDICOM.getProcessSessionFactory();
        ProcessContext context = listenDICOM.getProcessContext();
        listenProcessor.startDICOM(context, processSession);

        // Queue up a DICOM file
        InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
        putDICOM.enqueue(r);
        putDICOM.run();

        putDICOM.assertAllFlowFilesTransferred(PutDICOM.RELATIONSHIP_SUCCESS, 1);
        putDICOM.assertQueueEmpty();

        listenProcessor.stop();
        listenDICOM.assertAllFlowFilesTransferred(ListenDICOM.RELATIONSHIP_SUCCESS, 1);
    }

    @Test
    public void reject() throws IOException, GeneralSecurityException {
        // Start the listener
        ProcessSessionFactory processSession = listenDICOM.getProcessSessionFactory();
        ProcessContext context = listenDICOM.getProcessContext();
        listenProcessor.startDICOM(context, processSession);

        // Queue up a DICOM file
        putDICOM.enqueue("I am not a DICOM file!");
        putDICOM.setValidateExpressionUsage(false);

        putDICOM.run();
        putDICOM.assertQueueEmpty();
        putDICOM.assertAllFlowFilesTransferred(PutDICOM.RELATIONSHIP_REJECT, 1);

        listenProcessor.stop();
        listenDICOM.assertAllFlowFilesTransferred(ListenDICOM.RELATIONSHIP_SUCCESS, 0);
    }

    @Test
    public void someRejected() throws IOException, GeneralSecurityException {
        // Start the listener
        ProcessSessionFactory processSession = listenDICOM.getProcessSessionFactory();
        ProcessContext context = listenDICOM.getProcessContext();
        listenProcessor.startDICOM(context, processSession);

        // Queue up a DICOM file
        putDICOM.enqueue("I am not a DICOM file!");
        InputStream r;
        r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
        putDICOM.enqueue(r);
        r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_001.dcm");
        putDICOM.enqueue(r);
        r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_002.dcm");
        putDICOM.enqueue(r);

        putDICOM.setValidateExpressionUsage(false);

        putDICOM.run();
        putDICOM.assertQueueEmpty();
        assertEquals(0, putDICOM.getFlowFilesForRelationship(PutDICOM.RELATIONSHIP_FAILURE).size(), "expected 0 failure");
        assertEquals(1, putDICOM.getFlowFilesForRelationship(PutDICOM.RELATIONSHIP_REJECT).size(), "expected 1 reject");
        assertEquals(3, putDICOM.getFlowFilesForRelationship(PutDICOM.RELATIONSHIP_SUCCESS).size(), "expected 3 success");

        listenProcessor.stop();
        listenDICOM.assertAllFlowFilesTransferred(ListenDICOM.RELATIONSHIP_SUCCESS, 3);
    }

    @Test
    public void noListener() throws IOException, GeneralSecurityException {
        // Queue up a DICOM file
        putDICOM.enqueue("I am not a DICOM file!");
        InputStream r = getClass().getResourceAsStream("/dicom/LGG-104_SPGR_000.dcm");
        putDICOM.enqueue(r);
        putDICOM.setValidateExpressionUsage(false);

        putDICOM.run();
        putDICOM.assertQueueEmpty();

        assertEquals(1, putDICOM.getFlowFilesForRelationship(PutDICOM.RELATIONSHIP_FAILURE).size(), "expected 1 failure");
        assertEquals(1, putDICOM.getFlowFilesForRelationship(PutDICOM.RELATIONSHIP_REJECT).size(), "expected 1 reject");
    }

    static public int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
