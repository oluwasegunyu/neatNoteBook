<template>
  <a-layout>
    <a-layout-content :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }">
      <!--      搜素新增-->
      <a-form
          layout="inline"
          :model="param"
      >
        <a-form-item>
          <a-input v-model:value="param.name">
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-space size="small">
            <a-button
                type="primary"
                @click="handleQuery({page:1, size: pagination.pageSize})"
                html-type="submit"
            >
              查询
            </a-button>
            <a-button type="primary" @click="add()">
              新增
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <a-table
          :columns="columns"
          :data-source="ebooks"
          :row-key="record => record.id"
          :pagination="pagination"
          :loading="loading"
          @change="handleTableChange"
      >
        <template #cover="{ text:cover }">
          <img v-if="cover" :src="cover" alt="avatar"/>
        </template>
        <template v-slot:category1="{record}">
          <span>{{ getCategoryName(record.category1Id) }}</span>
        </template>
        <template v-slot:category2="{record}">
          <span>{{ getCategoryName(record.category2Id) }}</span>
        </template>
        <template v-slot:action="{record}">
          <a-space size="small">
            <a-button type="primary" @click="edit(record)">
              编辑
            </a-button>
            <a-popconfirm
                title="确认删除？"
                ok-text="是"
                cancel-text="否"
                @confirm="handleDelete(record.id)"
            >
              <a-button type="danger">
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-layout-content>
  </a-layout>

  <!--  新增表单  -->
  <a-modal v-model:visible="modalVisible" title="笔记表单" @ok="handleModalOk">
    <a-form :model="ebook" :label-col="labelCol" :wrapper-col="wrapperCol">
      <a-form-item label="封面">
        <a-input v-model:value="ebook.cover"/>
      </a-form-item>
      <a-form-item label="名称">
        <a-input v-model:value="ebook.name"/>
      </a-form-item>
      <a-form-item label="分类">
        <a-cascader
            v-model:value="categoryIds"
            :field-names="{label: 'name', value: 'id', children: 'children'}"
            :options="level1"
        />
      </a-form-item>
      <a-form-item label="描述">
        <a-input v-model:value="ebook.description"/>
      </a-form-item>
      <a-form-item>
        <a-form layout="inline">
          <a-form-item>
            <a-upload
                v-model:file-list="fileList"
                name="file"
                :multiple="false"
                action="http://localhost:8080/notebook"
                :headers="headers"
                @change="handleChange"
            >
              <a-button>
                <upload-outlined></upload-outlined>
                上传正文
              </a-button>
            </a-upload>
          </a-form-item>
          <a-form-item>
            <a-upload
                v-model:file-list="fileList"
                name="file"
                :multiple="true"
                action="https://www.mocky.io/v2/5cc8019d300000980a055e76"
                :headers="headers"
                @change="handleChange"
            >
              <a-button>
                <upload-outlined></upload-outlined>
                上传手稿
              </a-button>
            </a-upload>
          </a-form-item>
        </a-form>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts">
import {defineComponent, onMounted, ref} from 'vue';
import axios from "axios";
import {message} from 'ant-design-vue';
import {Tool} from "../../../util/tool";
import {UploadOutlined} from "@ant-design/icons-vue";


interface FileItem {
  uid: string;
  name?: string;
  status?: string;
  response?: string;
  url?: string;
}

interface FileInfo {
  file: FileItem;
  fileList: FileItem[];
}


export default defineComponent({
  name: 'AdminEbook',
  components: {
    UploadOutlined,
  },
  setup() {
    const param = ref();
    param.value = {};
    const ebooks = ref();
    const pagination = ref({
      current: 1,
      pageSize: 4,
      total: 0
    });
    const loading = ref(false);

    const columns = [
      {
        title: '封面',
        key: 'cover',
        dataIndex: 'cover',
        slots: {customRender: 'cover'},
      },
      {
        title: '名称',
        key: 'name',
        dataIndex: 'name',
      },
      {
        title: '类别',
        slots: {customRender: 'category1'}
      },
      {
        title: '子类',
        slots: {customRender: 'category2'}
      },
      {
        title: '阅读',
        dataIndex: 'viewCount',
      },
      {
        title: '点赞',
        dataIndex: 'voteCount',
      },
      {
        title: 'Action',
        key: 'action',
        slots: {customRender: 'action'},
      },
    ];

    /**
     * 数据查询
     */
    const handleQuery = (params: any) => {
      loading.value = true;
      //查询前清空，否则数据不刷新
      ebooks.value = [];
      axios.get("/ebook/list", {
        params: {
          page: params.page,
          size: params.size,
          name: param.value.name
        }
      }).then((response) => {
        loading.value = false;
        const data = response.data;
        if (data.success) {
          ebooks.value = data.content.list;

          pagination.value.current = params.page;
          pagination.value.total = data.content.total;
        } else {
          message.error(data.message);
        }
      });
    };

    /**
     * 点击页码触发
     */
    const handleTableChange = (pagination: any) => {
      handleQuery({
        page: pagination.current,
        size: pagination.pageSize
      });
    };

    //----表单-----
    const categoryIds = ref();
    const ebook = ref();
    const modalVisible = ref<boolean>(false);
    const showModal = () => {
      modalVisible.value = true;
    };
    const handleModalOk = () => {
      ebook.value.category1Id = categoryIds.value[0];
      ebook.value.category2Id = categoryIds.value[1];
      axios.post("/ebook/save", ebook.value).then((response) => {
        const data = response.data;
        if (data.success) {
          modalVisible.value = false;

          //重新加载列表
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize,
          });
        } else {
          message.error(data.message);
        }
      });
    };
    /**
     * add new note
     */
    const add = () => {
      modalVisible.value = true;
      ebook.value = {};
    }
    /**
     * 编辑
     */
    const edit = (record: any) => {
      modalVisible.value = true;
      ebook.value = Tool.copy(record);
      categoryIds.value = [ebook.value.category1Id, ebook.value.category2Id]
    }

    /**
     * delete the note
     */
    const handleDelete = (id: number) => {
      axios.delete("/ebook/delete/" + id).then((response) => {
        const data = response.data;
        if (data.success) {
          //重新加载列表
          handleQuery({
            page: pagination.value.current,
            size: pagination.value.pageSize,
          });
        }
      });
    }

    const level1 = ref();
    let categorys: any;
    /**
     * 查询所有分类
     **/
    const handleQueryCategory = () => {
      loading.value = true;
      axios.get("/category/all").then((response) => {
        loading.value = false;
        const data = response.data;
        if (data.success) {
          categorys = data.content;
          console.log("原始数组：", categorys);

          level1.value = [];
          level1.value = Tool.array2Tree(categorys, 0);
          console.log("树形结构：", level1.value);


          handleQuery({
            page: 1,
            size: pagination.value.pageSize
          });
        } else {
          message.error(data.message);
        }
      });
    };

    //获取类别名称
    const getCategoryName = (cid: number) => {
      let result = "";
      categorys.forEach((item: any) => {
        if (item.id === cid) {
          result = item.name;
        }
      });
      return result;
    };
    onMounted(() => {
      handleQueryCategory();

    });


    const handleChange = (info: FileInfo) => {
      if (info.file.status !== 'uploading') {
        console.log(info.file, info.fileList);
      }
      if (info.file.status === 'done') {
        message.success(`${info.file.name} file uploaded successfully`);
      } else if (info.file.status === 'error') {
        message.error(`${info.file.name} file upload failed.`);
      }
    };

    const fileList = ref([]);

    return {
      ebooks,
      pagination,
      loading,
      handleTableChange,
      columns,
      getCategoryName,

      //编辑
      ebook,
      edit,
      add,
      modalVisible,
      showModal,
      handleModalOk,
      handleDelete,
      param,
      handleQuery,
      categoryIds,
      level1,
    }
  }
});
</script>

<style scoped>
img {
  width: 50px;
  height: 50px;
}
</style>