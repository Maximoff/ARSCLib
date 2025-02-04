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
package com.reandroid.arsc.group;

import com.reandroid.arsc.base.BlockArrayCreator;
import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TypeBlock;
import com.reandroid.arsc.item.SpecString;
import com.reandroid.arsc.item.TypeString;
import com.reandroid.arsc.pool.SpecStringPool;
import com.reandroid.arsc.value.Entry;

import java.util.Iterator;

public class EntryGroup extends ItemGroup<Entry> {
    private final int resourceId;
    public EntryGroup(int resId) {
        super(create(), String.format("0x%08x", resId));
        this.resourceId=resId;
    }
    public int getResourceId(){
        return resourceId;
    }
    public boolean renameSpec(String name){
        Entry[] items=getItems();
        if(items==null || name==null){
            return false;
        }
        SpecStringPool specStringPool=getSpecStringPool();
        if(specStringPool==null){
            return false;
        }
        if(isAllSameSpec()){
            String oldName=getSpecName();
            if(name.equals(oldName)){
                return false;
            }
        }
        SpecString specString=specStringPool.getOrCreate(name);
        return renameSpec(specString.getIndex());
    }
    public short getEntryId(){
        return (short) (getResourceId() & 0xffff);
    }
    private boolean isAllSameSpec(){
        Entry first=null;
        for(Entry entry :listItems()){
            if(first==null){
                first= entry;
                continue;
            }
            if(first.getSpecReference()!= entry.getSpecReference()){
                return false;
            }
        }
        return true;
    }
    public boolean renameSpec(int specReference){
        Entry[] items=getItems();
        if(items==null){
            return false;
        }
        boolean renameOk=false;
        for(Entry block:items){
            if(block==null){
                continue;
            }
            if(block.getSpecReference()==specReference){
                continue;
            }
            block.setSpecReference(specReference);
            renameOk=true;
        }
        return renameOk;
    }
    public Entry pickOne(){
        Entry[] items=getItems();
        if(items==null){
            return null;
        }
        Entry result = null;
        for(Entry entry :items){
            if(entry ==null){
                continue;
            }
            if(result==null || result.isNull()){
                result= entry;
            }else if(entry.isDefault()){
                return entry;
            }
        }
        return result;
    }
    public Entry getDefault(){
        Iterator<Entry> itr=iterator(true);
        while (itr.hasNext()){
            Entry entry =itr.next();
            if(entry.isDefault()){
                return entry;
            }
        }
        return null;
    }
    public TypeString getTypeString(){
        Entry entry =pickOne();
        if(entry !=null){
            return entry.getTypeString();
        }
        return null;
    }
    public SpecString getSpecString(){
        Entry entry =pickOne();
        if(entry !=null){
            return entry.getSpecString();
        }
        return null;
    }
    public String getTypeName(){
        TypeString typeString=getTypeString();
        if(typeString==null){
            return null;
        }
        return typeString.get();
    }
    public String getSpecName(){
        SpecString specString=getSpecString();
        if(specString==null){
            return null;
        }
        return specString.get();
    }
    private SpecStringPool getSpecStringPool(){
        Entry entry =get(0);
        if(entry ==null){
            return null;
        }
        TypeBlock typeBlock= entry.getTypeBlock();
        if(typeBlock==null){
            return null;
        }
        PackageBlock packageBlock=typeBlock.getPackageBlock();
        if(packageBlock==null){
            return null;
        }
        return packageBlock.getSpecStringPool();
    }
    @Override
    public String toString(){
        Entry entry =pickOne();
        if(entry ==null){
            return super.toString();
        }
        return super.toString()+"{"+ entry.toString()+"}";
    }
    private static BlockArrayCreator<Entry> create(){
        return new BlockArrayCreator<Entry>(){
            @Override
            public Entry newInstance() {
                return new Entry();
            }

            @Override
            public Entry[] newInstance(int len) {
                return new Entry[len];
            }
        };
    }

}
