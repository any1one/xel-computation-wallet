package org.xel.computation;

import org.xel.*;
import org.xel.util.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.xel.computation.ComputationConstants.MAX_UNCOMPRESSED_WORK_SIZE;

/******************************************************************************
 * Copyright © 2017 The XEL Core Developers.                                  *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * XEL software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

public class CommandNewWork extends IComputationAttachment {

    private short deadline;

    public long getXelPerPow() {
        return xelPerPow;
    }

    public long getXelPerBounty() {
        return xelPerBounty;
    }

    public int getCap_number_pow() {
        return cap_number_pow;
    }

    public int getBountiesPerIteration() {
        return bountiesPerIteration;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public byte[] getSourceCode() {
        return sourceCode;
    }

    private long xelPerPow;
    private long xelPerBounty;
    private int cap_number_pow;

    public short getDeadline() {
        return deadline;
    }

    private int bountiesPerIteration;
    private int numberOfIterations;
    boolean validated = false;
    private byte[] sourceCode;
    private byte[] sourceCodeCompressed;

    private int storage_size = -1;

    public CommandNewWork(int cap_number_pow, short deadline, long xelPerPow, long xelPerBounty, int
            bountiesPerIteration, int numberOfIterations, byte[] sourceCode) {
        super();
        this.cap_number_pow = cap_number_pow;
        this.deadline = deadline;
        this.xelPerPow = xelPerPow;
        this.xelPerBounty = xelPerBounty;
        this.bountiesPerIteration = bountiesPerIteration;
        this.numberOfIterations = numberOfIterations;
        if (sourceCode.length <= MAX_UNCOMPRESSED_WORK_SIZE)
            this.sourceCode = sourceCode;
        else
            this.sourceCode = new byte[0];

        try {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(sourceCode);
            gzip.flush();
            gzip.close();
            sourceCodeCompressed = obj.toByteArray();
        } catch (Exception e) {
            sourceCodeCompressed = new byte[0];
        }
    }


    public CommandNewWork(int cap_number_pow, short deadline, long xelPerPow, long xelPerBounty, int bountiesPerIteration, int numberOfIterations, String sourceCode) throws UnsupportedEncodingException {
        this(cap_number_pow, deadline, xelPerPow, xelPerBounty, bountiesPerIteration, numberOfIterations, sourceCode
                .getBytes("UTF-8"));
    }

    CommandNewWork(ByteBuffer buffer) {
        super(buffer);
        byte compressed_or_not = 0; // assume false
        try {
            compressed_or_not = buffer.get();
            this.deadline = buffer.getShort();
            this.xelPerPow = buffer.getLong();
            this.xelPerBounty = buffer.getLong();
            this.bountiesPerIteration = buffer.getInt();
            this.numberOfIterations = buffer.getInt();
            this.cap_number_pow = buffer.getInt();

            if (compressed_or_not == 0) {
                short len = buffer.getShort();
                if (len > MAX_UNCOMPRESSED_WORK_SIZE)
                    this.sourceCode = new byte[0];
                else
                    this.sourceCode = new byte[len];

                if (this.sourceCode.length > 0)
                    buffer.get(this.sourceCode);


                ByteArrayOutputStream obj = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(obj);
                gzip.write(sourceCode);
                gzip.flush();
                gzip.close();
                sourceCodeCompressed = obj.toByteArray();

            } else {
                this.sourceCodeCompressed = new byte[buffer.getShort()];
                buffer.get(this.sourceCodeCompressed);
                if ((this.sourceCodeCompressed == null) || (this.sourceCodeCompressed.length == 0)) return;
                try {
                    if (isCompressed(this.sourceCodeCompressed)) {
                        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(this.sourceCodeCompressed));
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
                        String outStr = "";
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            outStr += line + "\n";
                        }
                        this.sourceCode = outStr.getBytes("UTF-8");
                        if (this.sourceCode.length > MAX_UNCOMPRESSED_WORK_SIZE)
                            this.sourceCode = new byte[0];
                    }
                } catch (Exception e) {
                    this.sourceCode = null;
                }
            }
        } catch (Exception e) {
            // pass through any error
            this.deadline = 0;
            this.xelPerPow = 0;
            this.xelPerBounty = 0;
            this.cap_number_pow = 0;
            this.bountiesPerIteration = 0;
            this.numberOfIterations = 0;
            this.sourceCode = null;
        }
    }

    private boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    @Override
    String getAppendixName() {
        return "CommandNewWork";
    }

    @Override
    int getMySize() {
        int min = 0;
        if (sourceCodeCompressed == null)
            min = this.sourceCode.length;
        else
            min = (this.sourceCode.length < this.sourceCodeCompressed.length) ? this.sourceCode.length : this.sourceCodeCompressed.length;
        return 1 + 2 + 8 + 8 + 4 + 4 + 4 + 2 + min;
    }

    @Override
    byte getMyMessageIdentifier() {
        return CommandsEnum.CREATE_NEW_WORK.getCode();
    }

    @Override
    public String toString() {
        return "Test (DL: " + this.deadline + "): " + new String(sourceCode);
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {

        boolean compressed = false;
        if (this.sourceCodeCompressed != null && this.sourceCodeCompressed.length < this.sourceCode.length) {
            compressed = true;
        }
        buffer.put((byte) (compressed ? 0x01 : 0x00));
        buffer.putShort(this.deadline);
        buffer.putLong(this.xelPerPow);
        buffer.putLong(this.xelPerBounty);
        buffer.putInt(this.bountiesPerIteration);
        buffer.putInt(this.numberOfIterations);
        buffer.putInt(this.cap_number_pow);

        if (compressed) {
            buffer.putShort((short) this.sourceCodeCompressed.length);
            buffer.put(this.sourceCodeCompressed);
        } else {
            buffer.putShort((short) this.sourceCode.length);
            buffer.put(this.sourceCode);
        }
    }

    public boolean validatePublic(Transaction transaction) {
        return validate(transaction);
    }


    public void checkAccountRestrictions(long senderId) throws NxtException.NotValidException {
        // See if account is known on the main chain
        Account acc = Account.getAccount(senderId);
        if(acc == null){
            throw new NxtException.NotValidException("Account is not known on the main chain or has balance zero.");
        }

        // Check if account already has more than MAX_CONCURRENT_JOBS_PER_USER works open
        int openCount = Work.getActiveCount(acc.getId());
        if(openCount>=Constants.MAX_CONCURRENT_JOBS_PER_USER){
            throw new NxtException.NotValidException("This account already has " + Constants.MAX_CONCURRENT_JOBS_PER_USER + " or more jobs running, wait until some of them are finished.");
        }

        // Check if Balance is fine
        long currentGrabsOpen = Work.getGrabsByUser(senderId); // this is the amout STILL open in all open works by the user
        long currentBalance = acc.getBalanceNQT();
        long thisJobWillCost = xelPerPow * cap_number_pow + (numberOfIterations*bountiesPerIteration*xelPerBounty);
        long totalBalanceRequired = currentGrabsOpen + thisJobWillCost;

        if(totalBalanceRequired<currentGrabsOpen || totalBalanceRequired<thisJobWillCost){
            throw new NxtException.NotValidException("No integer overflow attacks please, kiddo!");
        }
        if(totalBalanceRequired>currentBalance){
            throw new NxtException.NotValidException("You should top up your XEL balance, there is no way you could pay for this (and other jobs) at the moment.");
        }

    }


    @Override
    boolean validate(Transaction transaction) {
        if (transaction != null && transaction.getDeadline() > ComputationConstants.WORK_TRANSACTION_DEADLINE_VALUE)
            return false;
        if (((this.sourceCode == null) || (this.sourceCode.length == 0)) && (this.sourceCodeCompressed == null) || (this.sourceCodeCompressed.length == 0))
            return false;

        if (!Commons.checkRange(ComputationConstants.DEADLINE_MIN, ComputationConstants.DEADLINE_MAX, this.deadline))
            return false;

        if (!Commons.checkRange(ComputationConstants.XEL_POW_MIN, ComputationConstants.XEL_POW_MAX, this.xelPerPow))
            return false;

        if (!Commons.checkRange(ComputationConstants.XEL_BTY_MIN, ComputationConstants.XEL_BTY_MAX, this.xelPerBounty))
            return false;

        if (!Commons.checkRange(ComputationConstants.BTY_PER_ITER_MIN, ComputationConstants.BTY_PER_ITER_MAX, this.bountiesPerIteration))
            return false;

        if (!Commons.checkRange(ComputationConstants.ITER_MIN, ComputationConstants.ITER_MAX, this.numberOfIterations))
            return false;

        if (!Commons.checkRange(ComputationConstants.POW_MIN, ComputationConstants.POW_MAX, this.cap_number_pow))
            return false;

        if (this.sourceCode!=null && this.sourceCode.length > MAX_UNCOMPRESSED_WORK_SIZE) return false;

        // Check if account restrictions kick in
        try{
            if (transaction != null)
                this.checkAccountRestrictions(transaction.getSenderId());
        }catch(NxtException.NotValidException e){
            Logger.logInfoMessage("work package dropped: (reason: did not meet account requirements)");
            return false;
        }

        // Now, we have to validate whether the source code makes sense at all and meets the required WCET criteria
        // for the main as well as for the verify part. We can do it with a dummy compute call
        // The reason is, that the compute call will fail if syntax errors are present or if WCET boundaries are violated
        String id = "not yet brdcst";
        if (transaction != null) {
            id = Long.toUnsignedString(transaction.getId());
        }
        try {

            ExecutionEngine e = new ExecutionEngine();
            byte[] target = e.getMaximumTargetForTesting();
            target[0] = 0x49; // make it a bit more difficult
            byte[] publicKey = new byte[]{(byte) 0xF1, (byte) 0x6D, (byte) 0x48, (byte) 0x25, (byte) 0x0C, (byte) 0xE2, (byte) 0xA2, (byte) 0xA4, (byte) 0xFD, (byte) 0x4D, (byte) 0x9B, (byte) 0x08, (byte) 0x57, (byte) 0x7B, (byte) 0x2D, (byte) 0x3F, (byte) 0x92, (byte) 0xC6, (byte) 0x4D, (byte) 0x09, (byte) 0x3C, (byte) 0xD9, (byte) 0x68, (byte) 0xE6, (byte) 0xC7, (byte) 0x32, (byte) 0x5E, (byte) 0x40, (byte) 0x30, (byte) 0xB7, (byte) 0xF2, (byte) 0x06};
            long blockId = 123456789;
            long workId = -1;
            byte[] multi = publicKey; // leave it empty ffs
            int storage_id = -1;
            ComputationResult r = e.compute(target, publicKey, blockId, multi, workId, new String(this.getSourceCode()), storage_id, true);
            storage_size = r.storage_size;

            // HERE, DOUBLE CHECK FOR STORAGE SIZE IN CASE IT SLIPPED THROUGH -  IT CAN BE CRITICAL
            if (storage_size > ComputationConstants.MAX_STORAGE_SIZE) {
                Logger.logInfoMessage("work package dropped: id=" + id + " (reason: requested storage size is way too high)");
                return false;
            }

            validated = true;
        } catch (Exception e) {
            Logger.logInfoMessage("work package dropped: id=" + id + " (reason: " + e.getMessage() + ")");
            return false;
        }

        return true;
    }

    @Override
    void apply(Transaction transaction) {
        if ((this.sourceCode == null) || (this.sourceCode.length == 0)) return;

        if (!validated) {
            if (!validate(transaction))
                return;
        }
        // Here, apply the actual package
        Logger.logInfoMessage("new work package submitted: id=" + Long.toUnsignedString(transaction.getId()) + " (configures storage size: " + getStorageSize() + ")");
        Work.addWork(transaction, this);
    }

    public int getStorageSize() {
        if (storage_size == -1) {
            try {
                ExecutionEngine e = new ExecutionEngine();
                byte[] target = e.getMaximumTargetForTesting();
                target[0] = 0x49; // make it a bit more difficult
                byte[] publicKey = new byte[]{(byte) 0xF1, (byte) 0x6D, (byte) 0x48, (byte) 0x25, (byte) 0x0C, (byte) 0xE2, (byte) 0xA2, (byte) 0xA4, (byte) 0xFD, (byte) 0x4D, (byte) 0x9B, (byte) 0x08, (byte) 0x57, (byte) 0x7B, (byte) 0x2D, (byte) 0x3F, (byte) 0x92, (byte) 0xC6, (byte) 0x4D, (byte) 0x09, (byte) 0x3C, (byte) 0xD9, (byte) 0x68, (byte) 0xE6, (byte) 0xC7, (byte) 0x32, (byte) 0x5E, (byte) 0x40, (byte) 0x30, (byte) 0xB7, (byte) 0xF2, (byte) 0x06};
                long blockId = 123456789;
                long workId = -1;
                byte[] multi = publicKey; // leave it empty ffs
                int storage_id = -1;
                ComputationResult r = e.compute(target, publicKey, blockId, multi, workId, storage_id, true);
                validated = true;
                storage_size = r.storage_size;
            } catch (Exception e) {
                return -1;
            }
        }
        return storage_size;
    }
}
