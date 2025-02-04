 /*
  *  Copyright (C) 2022 github.com/REAndroid
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package com.reandroid.arsc.chunk;

import com.reandroid.arsc.BuildInfo;
import com.reandroid.arsc.array.PackageArray;
import com.reandroid.arsc.group.EntryGroup;
import com.reandroid.arsc.header.HeaderBlock;
import com.reandroid.arsc.header.InfoHeader;
import com.reandroid.arsc.header.TableHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.pool.StringPool;
import com.reandroid.arsc.pool.TableStringPool;
import com.reandroid.arsc.value.StagedAliasEntry;
import com.reandroid.common.Frameworks;
import com.reandroid.json.JSONConvert;
import com.reandroid.json.JSONArray;
import com.reandroid.json.JSONObject;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TableBlock extends Chunk<TableHeader>
        implements MainChunk, JSONConvert<JSONObject> {
    private final TableStringPool mTableStringPool;
    private final PackageArray mPackageArray;
    private final Set<TableBlock> mFrameWorks=new HashSet<>();
    public TableBlock() {
        super(new TableHeader(), 2);
        TableHeader header = getHeaderBlock();
        this.mTableStringPool=new TableStringPool(true);
        this.mPackageArray=new PackageArray(header.getPackageCount());
        addChild(mTableStringPool);
        addChild(mPackageArray);
    }
    public PackageBlock pickOne(){
        return getPackageArray().pickOne();
    }
    public void sortPackages(){
        getPackageArray().sort();
    }
    public Collection<PackageBlock> listPackages(){
        return getPackageArray().listItems();
    }
    @Override
    public TableStringPool getStringPool() {
        return mTableStringPool;
    }
    public TableStringPool getTableStringPool(){
        return mTableStringPool;
    }
    public PackageBlock getPackageBlockById(int pkgId){
        return getPackageArray().getPackageBlockById(pkgId);
    }
    public PackageArray getPackageArray(){
        return mPackageArray;
    }

    private void refreshPackageCount(){
        int count = getPackageArray().childesCount();
        getHeaderBlock().getPackageCount().set(count);
    }
    @Override
    protected void onChunkRefreshed() {
        refreshPackageCount();
    }
    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        TableHeader tableHeader = getHeaderBlock();
        tableHeader.readBytes(reader);
        if(tableHeader.getChunkType()!=ChunkType.TABLE){
            throw new IOException("Not resource table: "+tableHeader);
        }
        boolean stringPoolLoaded=false;
        InfoHeader infoHeader = reader.readHeaderBlock();
        PackageArray packageArray=mPackageArray;
        packageArray.clearChildes();
        while(infoHeader!=null && reader.isAvailable()){
            ChunkType chunkType=infoHeader.getChunkType();
            if(chunkType==ChunkType.STRING){
                if(!stringPoolLoaded){
                    mTableStringPool.readBytes(reader);
                    stringPoolLoaded=true;
                }
            }else if(chunkType==ChunkType.PACKAGE){
                PackageBlock packageBlock=packageArray.createNext();
                packageBlock.readBytes(reader);
            }else {
                UnknownChunk unknownChunk=new UnknownChunk();
                unknownChunk.readBytes(reader);
                addChild(unknownChunk);
            }
            infoHeader=reader.readHeaderBlock();
        }
        reader.close();
    }

    public void readBytes(File file) throws IOException{
        BlockReader reader=new BlockReader(file);
        super.readBytes(reader);
    }
    public void readBytes(InputStream inputStream) throws IOException{
        BlockReader reader=new BlockReader(inputStream);
        super.readBytes(reader);
    }
    public final int writeBytes(File file) throws IOException{
        if(isNull()){
            throw new IOException("Can NOT save null block");
        }
        File dir=file.getParentFile();
        if(dir!=null && !dir.exists()){
            dir.mkdirs();
        }
        OutputStream outputStream=new FileOutputStream(file);
        int length = super.writeBytes(outputStream);
        outputStream.close();
        return length;
    }

    @Override
    public String toString(){
        StringBuilder builder=new StringBuilder();
        builder.append(super.toString());
        builder.append(", packages=");
        int pkgCount=mPackageArray.childesCount();
        builder.append(pkgCount);
        return builder.toString();
    }
    public EntryGroup search(int resourceId){
        if(resourceId==0){
            return null;
        }
        int aliasId = searchResourceIdAlias(resourceId);
        for(PackageBlock packageBlock:listPackages()){
            EntryGroup entryGroup = packageBlock.getEntryGroup(resourceId);
            if(entryGroup!=null){
                return entryGroup;
            }
            entryGroup = packageBlock.getEntryGroup(aliasId);
            if(entryGroup!=null){
                return entryGroup;
            }
        }
        for(TableBlock tableBlock:getFrameWorks()){
            EntryGroup entryGroup = tableBlock.search(resourceId);
            if(entryGroup!=null){
                return entryGroup;
            }
        }
        return null;
    }
    public int searchResourceIdAlias(int resourceId){
        for(PackageBlock packageBlock:listPackages()){
            StagedAliasEntry stagedAliasEntry =
                    packageBlock.searchByStagedResId(resourceId);
            if(stagedAliasEntry!=null){
                return stagedAliasEntry.getFinalizedResId();
            }
        }
        return 0;
    }
    public Set<TableBlock> getFrameWorks(){
        return mFrameWorks;
    }
    public void addFramework(TableBlock tableBlock){
        if(tableBlock==null||tableBlock==this){
            return;
        }
        for(TableBlock frm:tableBlock.getFrameWorks()){
            if(frm==this){
                return;
            }
        }
        mFrameWorks.add(tableBlock);
    }
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject=new JSONObject();

        jsonObject.put(BuildInfo.NAME_arsc_lib_version, BuildInfo.getVersion());

        jsonObject.put(NAME_packages, getPackageArray().toJson());
        JSONArray jsonArray = getTableStringPool().toJson();
        if(jsonArray!=null){
            jsonObject.put(NAME_styled_strings, jsonArray);
        }
        return jsonObject;
    }
    @Override
    public void fromJson(JSONObject json) {
        getPackageArray().fromJson(json.getJSONArray(NAME_packages));
        refresh();
    }
    public void merge(TableBlock tableBlock){
        if(tableBlock==null||tableBlock==this){
            return;
        }
        if(getPackageArray().childesCount()==0 && getTableStringPool().countStrings()==0){
            getTableStringPool().merge(tableBlock.getTableStringPool());
        }
        getPackageArray().merge(tableBlock.getPackageArray());
        refresh();
    }
    public static TableBlock loadWithAndroidFramework(InputStream inputStream) throws IOException{
        TableBlock tableBlock=load(inputStream);
        tableBlock.addFramework(Frameworks.getAndroid());
        return tableBlock;
    }
    public static TableBlock load(File file) throws IOException{
        return load(new FileInputStream(file));
    }
    public static TableBlock load(InputStream inputStream) throws IOException{
        TableBlock tableBlock=new TableBlock();
        tableBlock.readBytes(inputStream);
        return tableBlock;
    }

    public static boolean isResTableBlock(File file){
        if(file==null){
            return false;
        }
        boolean result=false;
        try {
            InputStream inputStream=new FileInputStream(file);
            result=isResTableBlock(inputStream);
            inputStream.close();
        } catch (IOException ignored) {
        }
        return result;
    }
    public static boolean isResTableBlock(InputStream inputStream){
        try {
            HeaderBlock headerBlock= BlockReader.readHeaderBlock(inputStream);
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(BlockReader blockReader){
        if(blockReader==null){
            return false;
        }
        try {
            HeaderBlock headerBlock = blockReader.readHeaderBlock();
            return isResTableBlock(headerBlock);
        } catch (IOException ignored) {
            return false;
        }
    }
    public static boolean isResTableBlock(HeaderBlock headerBlock){
        if(headerBlock==null){
            return false;
        }
        ChunkType chunkType=headerBlock.getChunkType();
        return chunkType==ChunkType.TABLE;
    }
    public static final String FILE_NAME="resources.arsc";

    private static final String NAME_packages="packages";
    public static final String NAME_styled_strings="styled_strings";
}
