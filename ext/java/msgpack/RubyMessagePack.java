//
// MessagePack for Ruby
//
// Copyright (C) 2008-2012 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package msgpack;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import msgpack.template.RubyObjectTemplate;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;
import org.msgpack.unpacker.BufferUnpacker;


@JRubyModule(name = "MessagePack")
public class RubyMessagePack {

    private static Map<Ruby, MessagePack> msgpacks = new HashMap<Ruby, MessagePack>();

    public static MessagePack getMessagePack(Ruby runtime) {
	MessagePack msgpack = msgpacks.get(runtime);
	if (msgpack == null) {
	    msgpack = new MessagePack();
	    msgpacks.put(runtime, msgpack);
	}
	return msgpack;
    }

    @JRubyMethod(name = "pack", required = 1, optional = 1, module = true)
    public static IRubyObject pack(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        IRubyObject object = args[0];
        IRubyObject io = null;

        if (args.length == 2 && args[1].respondsTo("write")) {
            io = args[1];
        }

        try {
            if (io != null) {
        	throw runtime.newNotImplementedError("io != null"); // FIXME #MN
            }

            BufferPacker packer = getMessagePack(runtime).createBufferPacker();
            RubyObjectTemplate tmpl = new RubyObjectTemplate(runtime);
            tmpl.write(packer, object);
            RubyString result = RubyString.newString(runtime, new ByteList(packer.toByteArray()));
            result.setTaint(object.isTaint());
            result.setUntrusted(object.isUntrusted());
            return result;
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = "unpack", required = 1, module = true)
    public static IRubyObject unpack(ThreadContext context, IRubyObject recv, IRubyObject io) {
	Ruby runtime = context.getRuntime();
        IRubyObject v = io.checkStringType();

        try {
            if (v.isNil()) {
        	throw runtime.newTypeError("instance of IO needed");
            }

            ByteList bytes = ((RubyString) v).getByteList();
            BufferUnpacker unpacker = getMessagePack(runtime).createBufferUnpacker();
            unpacker.setArraySizeLimit(131071);
            unpacker.wrap(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
            RubyObjectTemplate tmpl = new RubyObjectTemplate(runtime);
            IRubyObject object = tmpl.read(unpacker, null);
            object.setTaint(io.isTaint());
            object.setUntrusted(io.isUntrusted());
            return object;
        } catch (EOFException e) {
            if (io.respondsTo("to_str")) {
        	throw runtime.newArgumentError("packed data too short");
            }
            throw runtime.newEOFError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }

    @JRubyMethod(name = "unpack_limit", required = 2, module = true)
    public static IRubyObject unpackLimit(ThreadContext context, IRubyObject recv, IRubyObject io, IRubyObject limit) {
        throw context.getRuntime().newNotImplementedError("unpack_limit"); // TODO #MN
    }
}
