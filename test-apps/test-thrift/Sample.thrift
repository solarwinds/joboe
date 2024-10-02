namespace py sample
namespace java sample


struct Message {
    1: required i32 msgID;
    2: required string text;
}

service Sample {
    void ping(),
    bool sendMessage(1:Message msg);
}
